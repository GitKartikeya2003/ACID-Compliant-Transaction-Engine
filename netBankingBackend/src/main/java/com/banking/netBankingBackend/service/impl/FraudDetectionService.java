package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.enums.RuleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {


    private final RedisTemplate<String, String> redisTemplate;
    private final FraudAlertPersistenceService fraudAlertPersistenceService;


    private static final BigDecimal LARGE_TRANSFER_THRESHOLD = new BigDecimal("100000");
    private static final int VELOCITY_LIMIT = 5;           // max transfers per window
    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(10);
    private static final BigDecimal DRAIN_THRESHOLD = new BigDecimal("0.90"); // 90% of balance

    private static final int BRUTE_FORCE_LIMIT = 3;
    private static final Duration BRUTE_FORCE_WINDOW = Duration.ofMinutes(5);


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransactionCompletedEvent event) {
        // each rule is wrapped so one rule throwing doesn't stop the others from running
        try {
            checkVelocity(event);
        } catch (Exception e) {
            log.error("Velocity check failed for account {}", event.getAccount().getAccountNumber(), e);
        }
        try {
            checkLargeTransfer(event);
        } catch (Exception e) {
            log.error("Large transfer check failed for account {}", event.getAccount().getAccountNumber(), e);
        }
        try {
            checkAccountDrain(event);
        } catch (Exception e) {
            log.error("Account drain check failed for account {}", event.getAccount().getAccountNumber(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleInsufficientFunds(TransactionInsufficientFundsEvent event) {
        try {
            checkAccountDrain(event.getAccount(), event.getAttemptedAmount());
        } catch (Exception e) {
            log.error("Account drain check (insufficient funds) failed for account {}",
                    event.getAccount().getAccountNumber(), e);
        }
    }

    // Rule 1 - too many transfers inside a sliding window (account takeover pattern)
    private void checkVelocity(TransactionCompletedEvent event) {

        String accountNo = event.getAccount().getAccountNumber();
        String key = "velocity:" + accountNo;

        Long count = redisTemplate.opsForValue().increment(key);

        // this is the FIRST transfer in a fresh window -> set the TTL now.
        // without this the key lives forever and "count" stops meaning
        // "transfers in the last N minutes" and starts meaning "transfers ever".
        if (count != null && count == 1L) {
            redisTemplate.expire(key, VELOCITY_WINDOW.toMillis(), TimeUnit.MILLISECONDS);
        }

        if (count != null && count > VELOCITY_LIMIT) {

            log.warn("Velocity rule fired for account {}: {} transfers within {} minutes",
                    accountNo, count, VELOCITY_WINDOW.toMinutes());

            fraudAlertPersistenceService.createAlert(
                    event.getAccount(),
                    RuleType.VELOCITY,
                    count + " transfers in " + VELOCITY_WINDOW.toMinutes()
                            + " minutes exceeds limit of " + VELOCITY_LIMIT
            );
        }
    }

    // Rule 2 - amount exceeds threshold
    private void checkLargeTransfer(TransactionCompletedEvent event) {
        if (event.getAmount().compareTo(LARGE_TRANSFER_THRESHOLD) > 0) {

            log.warn("Large transfer rule fired for account {}: amount {}",
                    event.getAccount().getAccountNumber(), event.getAmount());

            fraudAlertPersistenceService.createAlert(
                    event.getAccount(),
                    RuleType.LARGE_TRANSFER,
                    "Transfer of " + event.getAmount() + " exceeds threshold of " + LARGE_TRANSFER_THRESHOLD
            );
        }
    }

    // Rule 3 - transfer drains most of the balance
    private void checkAccountDrain(TransactionCompletedEvent event) {

        BigDecimal balanceBefore = event.getBalanceBeforeTransfer();


        if (balanceBefore == null || balanceBefore.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal drainRatio = event.getAmount().divide(balanceBefore, 4, RoundingMode.HALF_UP);

        if (drainRatio.compareTo(DRAIN_THRESHOLD) >= 0) {

            log.warn("Account drain rule fired for account {}: {}% of balance drained",
                    event.getAccount().getAccountNumber(),
                    drainRatio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));

            fraudAlertPersistenceService.createAlert(
                    event.getAccount(),
                    RuleType.ACCOUNT_DRAIN,
                    "Transfer of " + event.getAmount() + " drained "
                            + drainRatio.multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP) + "% of balance"
            );
        }
    }

    private void checkAccountDrain(AccountEntity account, BigDecimal attemptedAmount) {

        BigDecimal currentBalance = account.getBalance();

        if (currentBalance == null || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal drainRatio = attemptedAmount.divide(currentBalance, 4, RoundingMode.HALF_UP);

        if (drainRatio.compareTo(DRAIN_THRESHOLD) >= 0) {

            log.warn("Account drain (insufficient funds) fired for account {}: attempted {}% of balance",
                    account.getAccountNumber(),
                    drainRatio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));

            fraudAlertPersistenceService.createAlert(
                    account,
                    RuleType.ACCOUNT_DRAIN,
                    "Attempted transfer of " + attemptedAmount + " would have drained "
                            + drainRatio.multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP) + "% of balance (funds unavailable)"
            );
        }
    }



    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onPinFailed(TransactionPinFailedEvent event) {

        try {

            checkBruteForce(event);


        } catch (Exception e) {
            log.error("Brute force check failed for account {}",
                    event.getAccount().getAccountNumber(), e);
        }
    }


    private void checkBruteForce(TransactionPinFailedEvent event) {

        String accountNumber = event.getAccount().getAccountNumber();
        String key = "failed_attempts:" + accountNumber;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {

            redisTemplate.expire(key, BRUTE_FORCE_WINDOW.toMillis(), TimeUnit.MILLISECONDS);
        }


        if (count != null && count >= BRUTE_FORCE_LIMIT) {

            log.warn("Brute force rule fired for account {}: {} failed PIN attempts in {} minutes",
                    accountNumber, count, BRUTE_FORCE_WINDOW.toMinutes());

            fraudAlertPersistenceService.createAlert(
                    event.getAccount(),
                    RuleType.BRUTE_FORCE,
                    count + " failed PIN attempts in " + BRUTE_FORCE_WINDOW.toMinutes()
                            + " minutes exceeds limit of " + BRUTE_FORCE_LIMIT
            );
        }

    }


}
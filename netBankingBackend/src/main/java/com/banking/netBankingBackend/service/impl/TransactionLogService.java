package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.TransactionEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persists transaction log entries OUTSIDE the transfer's lock boundary.
 *
 * Previously, saveTransaction() ran inside @Transactional on TransferExecutor,
 * meaning the pessimistic row-locks on both accounts were held for the full
 * duration of the INSERT into the transactions table. Under 100 concurrent users
 * this serialised all transfers through a single lock queue.
 *
 * Now both listeners fire AFTER the transfer transaction commits (or rolls back),
 * so the DB locks are released as soon as the balance update is flushed — before
 * any logging I/O takes place. Each listener opens its own independent
 * REQUIRES_NEW transaction so a logging failure never rolls back the transfer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLogService {

    private final TransactionRepository transactionRepository;

    /**
     * Logs a SUCCESSFUL transfer after the transfer transaction commits.
     * Runs on the dedicated txnLogExecutor thread pool — never on a Tomcat thread.
     */
    @Async("txnLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferCompleted(TransactionCompletedEvent event) {
        try {
            saveTransaction(
                    event.getAccount(),
                    event.getToAccount(),
                    event.getAmount(),
                    Status.SUCCESS
            );
        } catch (Exception e) {
            log.error("Failed to persist SUCCESS transaction log for account {}: {}",
                    event.getAccount().getAccountNumber(), e.getMessage(), e);
        }
    }

    /**
     * Logs a FAILED transfer (insufficient funds) after the transfer transaction rolls back.
     * REQUIRES_NEW ensures this write commits even though the outer txn was rolled back.
     */
    @Async("txnLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferFailed(TransactionInsufficientFundsEvent event) {
        try {
            saveTransaction(
                    event.getAccount(),
                    event.getToAccount(),
                    event.getAttemptedAmount(),
                    Status.FAILED
            );
        } catch (Exception e) {
            log.error("Failed to persist FAILED transaction log for account {}: {}",
                    event.getAccount().getAccountNumber(), e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helper — not called from outside this class any more.
    // Kept private to prevent callers bypassing the async event pattern.
    // -----------------------------------------------------------------------
    private void saveTransaction(AccountEntity from, AccountEntity to,
                                 BigDecimal amount, Status status) {
        TransactionEntity txn = new TransactionEntity();
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setBalance(amount);
        txn.setStatus(status);
        txn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(txn);
        log.debug("Transaction log saved: status={} amount={} from={}", status, amount,
                from.getAccountNumber());
    }
}
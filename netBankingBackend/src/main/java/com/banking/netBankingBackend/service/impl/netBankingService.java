package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.exception.*;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class netBankingService implements INetBankingService {


    private final AccountsRepository accountsRepository;

    private final TransactionLogService transactionLogService;

    private final ApplicationEventPublisher eventPublisher;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", allEntries = true)
    public void createTransaction(TransactionDto transactionDto, String emailHash, String pin) {

        if (transactionDto.getFrom_accountNumber().equals(transactionDto.getTo_AccountNumber())) {
            log.warn("Rejected transfer: same account number {}", transactionDto.getFrom_accountNumber());
            throw new SameAccountTransferException("Cannot transfer to the same account");
        }

        if (transactionDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Rejected transfer: invalid amount {} from account {}",
                    transactionDto.getAmount(), transactionDto.getFrom_accountNumber());
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        String fromAccountNoHash = AESUtil.hash(transactionDto.getFrom_accountNumber());
        String toAccountHash = AESUtil.hash(transactionDto.getTo_AccountNumber());

        // --- Fast, unlocked pre-checks (ownership, frozen status, PIN) ---
        // Do these BEFORE taking any row lock so a bad PIN or frozen account
        // never holds a connection under lock contention.
        AccountEntity fromAccountUnlocked = accountsRepository.findByAccountHash(fromAccountNoHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (fromAccountUnlocked.getStatus() == AccountStatus.FROZEN) {
            throw new FrozenAccountException("Your Account has been frozen");
        }

        UserEntity user = fromAccountUnlocked.getUser();
        if (user.getEmailHash().compareTo(emailHash) != 0) {
            log.warn("Account no {} is not of user: {}", fromAccountUnlocked.getAccountNumber(), user.getEmail());
            throw new InvalidTransactionException(
                    "Account No: " + fromAccountUnlocked.getAccountNumber() + " does not exist in your ownership");
        }

        AccountEntity toAccountUnlocked = accountsRepository.findByAccountHash(toAccountHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (toAccountUnlocked.getStatus() == AccountStatus.FROZEN) {
            throw new FrozenAccountException("Receiver Account is Frozen");
        }

        log.info("Verifying PIN code for account");
        verifyTransactionPin(fromAccountUnlocked, pin); // BCrypt happens here, no lock held

        // --- Now take locks, in a FIXED order, to avoid deadlocks ---
        String firstHash = fromAccountNoHash.compareTo(toAccountHash) < 0 ? fromAccountNoHash : toAccountHash;
        String secondHash = firstHash.equals(fromAccountNoHash) ? toAccountHash : fromAccountNoHash;

        AccountEntity firstLocked = accountsRepository.findByAccountHashForUpdate(firstHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        AccountEntity secondLocked = accountsRepository.findByAccountHashForUpdate(secondHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountEntity fromAccount = firstHash.equals(fromAccountNoHash) ? firstLocked : secondLocked;
        AccountEntity toAccount = firstHash.equals(fromAccountNoHash) ? secondLocked : firstLocked;

        BigDecimal balanceBefore = fromAccount.getBalance();

        log.info("Transfer initiated: amount={} from account={} to account={}",
                transactionDto.getAmount(),
                transactionDto.getFrom_accountNumber(),
                transactionDto.getTo_AccountNumber());

        if (fromAccount.getBalance().compareTo(transactionDto.getAmount()) < 0) {
            transactionLogService.saveTransaction(fromAccount, toAccount, transactionDto.getAmount(), Status.FAILED);
            log.warn("Transfer FAILED: insufficient balance. Account={} has={} requested={}",
                    fromAccount.getAccountNumber(), fromAccount.getBalance(), transactionDto.getAmount());

            eventPublisher.publishEvent(
                    TransactionInsufficientFundsEvent.builder()
                            .account(fromAccount)
                            .attemptedAmount(transactionDto.getAmount())
                            .availableBalance(fromAccount.getBalance())
                            .timestamp(LocalDateTime.now())
                            .build()
            );
            throw new InsufficientBalanceException("Insufficient balance");
        }

        BigDecimal amountToSend = transactionDto.getAmount();
        fromAccount.setBalance(fromAccount.getBalance().subtract(amountToSend));
        toAccount.setBalance(toAccount.getBalance().add(amountToSend));

        transactionLogService.saveTransaction(fromAccount, toAccount, amountToSend, Status.SUCCESS);
        eventPublisher.publishEvent(
                new TransactionCompletedEvent(fromAccount, amountToSend, balanceBefore, LocalDateTime.now())
        );

        log.info("Transfer SUCCESS: amount={} from account={} (new balance={}) to account={} (new balance={})",
                amountToSend, fromAccount.getAccountNumber(), fromAccount.getBalance(),
                toAccount.getAccountNumber(), toAccount.getBalance());
    }

    // Inside your transfer service, BEFORE processing the transfer
    public void verifyTransactionPin(AccountEntity account, String enteredPin) {

        if (account.getTransactionPin() == null) {
            throw new PinNotSetException("Transaction PIN not set for this account");
        }

        if (!encoder.matches(enteredPin, account.getTransactionPin())) {

            eventPublisher.publishEvent(
                    TransactionPinFailedEvent.builder()
                            .account(account)
                            .failureReason("WRONG_PIN")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            throw new InvalidPinException("Invalid transaction PIN");
        }
    }


}

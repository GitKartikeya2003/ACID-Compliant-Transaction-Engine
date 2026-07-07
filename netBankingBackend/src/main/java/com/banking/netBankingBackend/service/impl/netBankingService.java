package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.exception.*;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import com.banking.netBankingBackend.service.impl.bankingImpl.TransferExecutor;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class netBankingService implements INetBankingService {

    private final AccountsRepository accountsRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final TransferExecutor transferExecutor;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);


    @Override
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


        AccountEntity fromAccount = accountsRepository.findByAccountHash(fromAccountNoHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (fromAccount.getStatus() == AccountStatus.FROZEN) {
            throw new FrozenAccountException("Your Account has been frozen");
        }

        if (fromAccount.getUser().getEmailHash().compareTo(emailHash) != 0) {
            log.warn("Account no does not belong to you");
            throw new InvalidTransactionException("Account does not belong to you");
        }

        if (!accountsRepository.existsByAccountHash(toAccountHash)) {
            log.warn("To Account Not found");
            throw new ResourceNotFoundException("Account not found");
        }

        log.info("Verifying PIN code for account");
        String storedPinHash = accountsRepository.findTransactionPinByHash(fromAccountNoHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (storedPinHash == null) {
            throw new PinNotSetException("Transaction PIN not set for this account");
        }

        if (!encoder.matches(storedPinHash, pin)) {

            eventPublisher.publishEvent(
                    TransactionPinFailedEvent.builder()
                            .account(fromAccount)
                            .failureReason("WRONG_PIN")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            throw new InvalidPinException("Invalid transaction PIN");
        }
        transferExecutor.executeTransfer(fromAccountNoHash, toAccountHash, transactionDto.getAmount());

//        // --- Now take locks, in a FIXED order, to avoid deadlocks ---
//        String firstHash = fromAccountNoHash.compareTo(toAccountHash) < 0 ? fromAccountNoHash : toAccountHash;
//        String secondHash = firstHash.equals(fromAccountNoHash) ? toAccountHash : fromAccountNoHash;
//
//        // Scoped to this transaction only — auto-resets on commit/rollback.
//        entityManager.createNativeQuery("SET LOCAL lock_timeout = '3000'").executeUpdate();
//
//        AccountEntity firstLocked = accountsRepository.findByAccountHashForUpdate(firstHash)
//                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
//        AccountEntity secondLocked = accountsRepository.findByAccountHashForUpdate(secondHash)
//                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
//
//        AccountEntity fromAccount = firstHash.equals(fromAccountNoHash) ? firstLocked : secondLocked;
//        AccountEntity toAccount = firstHash.equals(fromAccountNoHash) ? secondLocked : firstLocked;
//
//
//        entityManager.refresh(fromAccount);
//
//        if (toAccount.getStatus() == AccountStatus.FROZEN) {
//            throw new FrozenAccountException("Receiver Account is Frozen");
//        }
//
//        BigDecimal balanceBefore = fromAccount.getBalance();
//
//        log.info("Transfer initiated: amount={} from account={} to account={}",
//                transactionDto.getAmount(),
//                transactionDto.getFrom_accountNumber(),
//                transactionDto.getTo_AccountNumber());
//
//        if (fromAccount.getBalance().compareTo(transactionDto.getAmount()) < 0) {
//            transactionLogService.saveTransaction(fromAccount, toAccount, transactionDto.getAmount(), Status.FAILED);
//            log.warn("Transfer FAILED: insufficient balance. Account={} has={} requested={}",
//                    fromAccount.getAccountNumber(), fromAccount.getBalance(), transactionDto.getAmount());
//
//            eventPublisher.publishEvent(
//                    TransactionInsufficientFundsEvent.builder()
//                            .account(fromAccount)
//                            .attemptedAmount(transactionDto.getAmount())
//                            .availableBalance(fromAccount.getBalance())
//                            .timestamp(LocalDateTime.now())
//                            .build()
//            );
//            throw new InsufficientBalanceException("Insufficient balance");
//        }
//
//        BigDecimal amountToSend = transactionDto.getAmount();
//        fromAccount.setBalance(fromAccount.getBalance().subtract(amountToSend));
//        toAccount.setBalance(toAccount.getBalance().add(amountToSend));
//
//        transactionLogService.saveTransaction(fromAccount, toAccount, amountToSend, Status.SUCCESS);
//        eventPublisher.publishEvent(
//                new TransactionCompletedEvent(fromAccount, amountToSend, balanceBefore, LocalDateTime.now())
//        );
//
//        log.info("Transfer SUCCESS: amount={} from account={} (new balance={}) to account={} (new balance={})",
//                amountToSend, fromAccount.getAccountNumber(), fromAccount.getBalance(),
//                toAccount.getAccountNumber(), toAccount.getBalance());
    }

}
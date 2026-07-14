package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.TransactionEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLogService {

    private final TransactionRepository transactionRepository;
    private final CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;


    @Async("txnLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferCompleted(TransactionCompletedEvent event) {
        try {
            saveTransaction(event.getFromAccountId(), event.getToAccountId(),
                    event.getAmount(), Status.SUCCESS);
        } catch (Exception e) {
            log.error("Failed to persist SUCCESS transaction log: fromId={} toId={} amount={}: {}",
                    event.getFromAccountId(), event.getToAccountId(), event.getAmount(), e.getMessage(), e);
        }


        evictAccountCache(event.getAccount().getAccountNumber());
        // toAccount's account number requires a fresh DB read here; evict by ID lookup
        evictAccountCacheById(event.getToAccountId());
    }


    @Async("txnLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransferFailed(TransactionInsufficientFundsEvent event) {
        try {
            saveTransaction(event.getFromAccountId(), event.getToAccountId(),
                    event.getAttemptedAmount(), Status.FAILED);
        } catch (Exception e) {
            log.error("Failed to persist FAILED transaction log: fromId={} toId={} amount={}: {}",
                    event.getFromAccountId(), event.getToAccountId(), event.getAttemptedAmount(), e.getMessage(), e);
        }
    }


    private void saveTransaction(Long fromAccountId, Long toAccountId,
                                 BigDecimal amount, Status status) {
        // getReference() = FK-only proxy; never triggers UPDATE on accounts
        AccountEntity fromRef = entityManager.getReference(AccountEntity.class, fromAccountId);
        AccountEntity toRef   = entityManager.getReference(AccountEntity.class, toAccountId);

        TransactionEntity txn = new TransactionEntity();
        txn.setFromAccount(fromRef);
        txn.setToAccount(toRef);
        txn.setBalance(amount);
        txn.setStatus(status);
        txn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(txn);

        log.debug("Transaction log saved: status={} amount={} fromId={} toId={}",
                status, amount, fromAccountId, toAccountId);
    }

    private void evictAccountCache(String accountNo) {
        if (accountNo == null) return;
        Cache cache = cacheManager.getCache("accounts");
        if (cache != null) {
            cache.evict(accountNo);
        }
    }

    private void evictAccountCacheById(Long accountId) {
        // We need the plain-text account number to match the @Cacheable key.
        // Load a fresh reference — this is a SELECT, but it's on the txnLogExecutor
        // thread pool, not the Tomcat request thread, so it doesn't add latency.
        try {
            AccountEntity toAccount = entityManager.find(AccountEntity.class, accountId);
            if (toAccount != null) {
                evictAccountCache(toAccount.getAccountNumber());
            }
        } catch (Exception e) {
            log.warn("Could not evict cache for accountId={}: {}", accountId, e.getMessage());
        }
    }
}
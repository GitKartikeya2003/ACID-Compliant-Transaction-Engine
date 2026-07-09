package com.banking.netBankingBackend.service.impl.bankingImpl;


import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.exception.FrozenAccountException;
import com.banking.netBankingBackend.exception.InsufficientBalanceException;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferExecutor {

    private final AccountsRepository accountsRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;


    private void evictAccountCache(String accountNo) {
        Cache cache = cacheManager.getCache("accounts");
        if (cache != null) {
            cache.evict(accountNo);
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public void executeTransfer(String fromHash, String toHash, BigDecimal amount) {

        List<String> sortedHashes = Stream.of(fromHash, toHash).sorted().toList();


        entityManager.createNativeQuery("SET LOCAL lock_timeout = '3000'").executeUpdate();
        List<AccountEntity> locked = accountsRepository.findByAccountHashInForUpdate(sortedHashes);
        if (locked.size() != 2) {
            throw new ResourceNotFoundException("Account not found");
        }

        Map<String, AccountEntity> byHash = locked.stream()
                .collect(Collectors.toMap(AccountEntity::getAccountHash, a -> a));
        AccountEntity fromAccount = byHash.get(fromHash);
        AccountEntity toAccount = byHash.get(toHash);


        // Re check status inside the lock — account may have been frozen between
        if (fromAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Your Account has been frozen");
        if (toAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Receiver Account is Frozen");

        BigDecimal balanceBefore = fromAccount.getBalance();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer failed — insufficient balance");

            eventPublisher.publishEvent(TransactionInsufficientFundsEvent.builder()
                    .account(fromAccount)
                    .toAccount(toAccount)
                    .attemptedAmount(amount)
                    .availableBalance(fromAccount.getBalance())
                    .timestamp(LocalDateTime.now())
                    .build());

            throw new InsufficientBalanceException("Insufficient balance");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        eventPublisher.publishEvent(
                new TransactionCompletedEvent(fromAccount, toAccount, amount, balanceBefore, LocalDateTime.now())
        );

        // Evict only the two accounts that changed.
        evictAccountCache(fromAccount.getAccountNumber());
        evictAccountCache(toAccount.getAccountNumber());

        log.info("Transfer committed: amount={}", amount);
    }
}

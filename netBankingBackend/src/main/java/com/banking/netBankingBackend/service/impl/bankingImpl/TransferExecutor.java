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

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public void executeTransfer(String fromHash, String toHash, BigDecimal amount) {

        // Acquire locks in sorted hash order to prevent deadlocks
        List<String> sortedHashes = Stream.of(fromHash, toHash).sorted().toList();

        // Fail fast on lock contention rather than letting threads pile up
        entityManager.createNativeQuery("SET LOCAL lock_timeout = '3000'").executeUpdate();
        List<AccountEntity> locked = accountsRepository.findByAccountHashInForUpdate(sortedHashes);
        if (locked.size() != 2) {
            throw new ResourceNotFoundException("Account not found");
        }

        Map<String, AccountEntity> byHash = locked.stream()
                .collect(Collectors.toMap(AccountEntity::getAccountHash, a -> a));
        AccountEntity fromAccount = byHash.get(fromHash);
        AccountEntity toAccount = byHash.get(toHash);

        entityManager.refresh(fromAccount);
        entityManager.refresh(toAccount);

        // Re-check status inside lock — account may have been frozen between
        if (fromAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Your Account has been frozen");
        if (toAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Receiver Account is Frozen");

        BigDecimal balanceBefore = fromAccount.getBalance();

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer failed — insufficient balance. account={} has={} requested={}",
                    fromAccount.getId(), fromAccount.getBalance(), amount);


            eventPublisher.publishEvent(TransactionInsufficientFundsEvent.builder()
                    .fromAccountId(fromAccount.getId())
                    .toAccountId(toAccount.getId())
                    .account(fromAccount)               // read-only: for fraud rule checks
                    .attemptedAmount(amount)
                    .availableBalance(fromAccount.getBalance())
                    .timestamp(LocalDateTime.now())
                    .build());

            throw new InsufficientBalanceException("Insufficient balance");
        }

        // Mutation in balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        eventPublisher.publishEvent(
                new TransactionCompletedEvent(
                        fromAccount.getId(),       // fromAccountId
                        toAccount.getId(),         // toAccountId
                        fromAccount,
                        amount,
                        balanceBefore,
                        LocalDateTime.now()
                )
        );

        log.info("Transfer prepared: fromId={} toId={} amount={}", fromAccount.getId(), toAccount.getId(), amount);

    }
}

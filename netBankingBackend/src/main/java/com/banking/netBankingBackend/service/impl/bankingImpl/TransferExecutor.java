package com.banking.netBankingBackend.service.impl.bankingImpl;


import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.exception.FrozenAccountException;
import com.banking.netBankingBackend.exception.InsufficientBalanceException;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.impl.TransactionLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final TransactionLogService transactionLogService;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;


    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", allEntries = true)
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
        AccountEntity toAccount   = byHash.get(toHash);

       // entityManager.refresh(fromAccount);


        //Final checking maybe account got Frozen in between the transactions
        if (fromAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Your Account has been frozen");
        if (toAccount.getStatus() == AccountStatus.FROZEN)
            throw new FrozenAccountException("Receiver Account is Frozen");

        BigDecimal balanceBefore = fromAccount.getBalance();

        log.info("Transfer initiated:");

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer Failed");
            transactionLogService.saveTransaction(fromAccount, toAccount, amount, Status.FAILED);
            eventPublisher.publishEvent(TransactionInsufficientFundsEvent.builder()
                    .account(fromAccount).attemptedAmount(amount)
                    .availableBalance(fromAccount.getBalance())
                    .timestamp(LocalDateTime.now()).build());
            throw new InsufficientBalanceException("Insufficient balance");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        transactionLogService.saveTransaction(fromAccount, toAccount, amount, Status.SUCCESS);
        eventPublisher.publishEvent(new TransactionCompletedEvent(fromAccount, amount, balanceBefore, LocalDateTime.now()));


    }

}

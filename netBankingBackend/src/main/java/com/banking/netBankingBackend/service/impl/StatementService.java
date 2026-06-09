package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.Statement;
import com.banking.netBankingBackend.entity.TransactionEntity;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.StatementRepository;
import com.banking.netBankingBackend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final TransactionRepository transactionRepository;
    private final AccountsRepository accountsRepository;
    private final StatementRepository statementRepository;  // ← added

   // @Scheduled(cron = "0/30 * * * * ?")   // This is just for testing setting from 30 days to 30 seconds
    @Scheduled(cron = "0 0 1 1 * ?")
    public void generateMonthlyStatement() {

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int pageSize = 1000;
        int pageNumber = 0;

        Page<AccountEntity> accountPage;

        do {
            accountPage = accountsRepository
                    .findAll(PageRequest.of(pageNumber, pageSize));

            accountPage.getContent().forEach(account -> {
                List<TransactionEntity> transactions = transactionRepository
                        .findTransactionsForLast30Days(account, thirtyDaysAgo);
                generateStatement(account, transactions);  // ← recursive call removed
            });

            log.info("Processed batch {} — {} accounts",
                    pageNumber,
                    accountPage.getNumberOfElements());

            pageNumber++;

        } while (accountPage.hasNext());
    }

    private void generateStatement(AccountEntity account, List<TransactionEntity> transactions) {

        BigDecimal total = transactions.stream()
                .map(TransactionEntity::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Statement statement = new Statement();
        statement.setAccount(account);
        statement.setGeneratedAt(LocalDateTime.now());
        statement.setPeriodStart(LocalDateTime.now().minusDays(30));
        statement.setPeriodEnd(LocalDateTime.now());
        statement.setTotalTransactions(transactions.size());
        statement.setTotalAmountTransferred(total);

        statementRepository.save(statement);  // ← actually persists now

        log.info("========== STATEMENT FOR ACCOUNT: {} ==========", account.getId());
        log.info("Account Holder    : {}", account.getName());
        log.info("Period            : Last 30 days");
        log.info("Total Transactions: {}", transactions.size());
        log.info("Total Amount      : {}", total);
        log.info("===============================================");
    }
}
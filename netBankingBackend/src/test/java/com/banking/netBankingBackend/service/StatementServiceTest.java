package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.Statement;
import com.banking.netBankingBackend.entity.TransactionEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.StatementRepository;
import com.banking.netBankingBackend.repository.TransactionRepository;
import com.banking.netBankingBackend.service.impl.StatementService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
@Rollback
class StatementServiceTest {

    @BeforeAll
    static void setup() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired
    private StatementService statementService;

    @Autowired
    private AccountsRepository accountRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldGenerateStatementsForAllAccounts() {
        // ARRANGE
        AccountEntity account = new AccountEntity();
        account.setName("Test User");
        account.setAccountNumber("TEST1234567890");
        account.setBalance(new BigDecimal("10000"));
        AccountEntity savedAccount = accountRepository.save(account); // ← capture saved entity

        TransactionEntity txn = new TransactionEntity();
        txn.setFromAccount(savedAccount);   // ← use savedAccount
        txn.setToAccount(savedAccount);     // ← was missing entirely
        txn.setBalance(new BigDecimal("500"));
        txn.setTimestamp(LocalDateTime.now().minusDays(5));
        txn.setStatus(Status.SUCCESS);
        transactionRepository.save(txn);

        // ACT
        statementService.generateMonthlyStatement();

        // ASSERT
        List<Statement> statements = statementRepository.findByAccount(savedAccount); // ← use savedAccount

        assertFalse(statements.isEmpty());
        assertEquals(1, statements.getFirst().getTotalTransactions());
        assertEquals(0, new BigDecimal("500.00").compareTo(statements.getFirst().getTotalAmountTransferred()));
        //This is the right long-term fix anyway — BigDecimal equality should always use compareTo,
        // never assertEquals, because new BigDecimal("500").equals(new BigDecimal("500.00")) is false in Java by design.
    }
}
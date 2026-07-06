package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.TransactionEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.repository.TransactionRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionLogService {

    private final TransactionRepository transactionRepository;

    // Runs in its OWN transaction — survives the rollback above
    //@Transactional(value = Transactional.TxType.REQUIRES_NEW)
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveTransaction(AccountEntity from, AccountEntity to,
                                BigDecimal amount, Status status) {
        TransactionEntity txn = new TransactionEntity();
        txn.setFromAccount(from);
        txn.setToAccount(to);
        txn.setBalance(amount);
        txn.setStatus(status);
        txn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(txn);
    }
}
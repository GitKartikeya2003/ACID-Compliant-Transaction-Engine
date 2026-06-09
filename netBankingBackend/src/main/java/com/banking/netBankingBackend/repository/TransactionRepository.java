package com.banking.netBankingBackend.repository;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer>,
        RevisionRepository<TransactionEntity, Integer, Integer> {

    // Fetch transactions for a specific account in last 30 days
    @Query("SELECT t FROM TransactionEntity t WHERE t.fromAccount = :account AND t.timestamp >= :startDate")
    List<TransactionEntity> findTransactionsForLast30Days(
            @Param("account") AccountEntity account,
            @Param("startDate") LocalDateTime startDate
    );

//    // Fetch all transactions after a given date (for batch statement generation)
//    @Query("SELECT t FROM TransactionEntity t WHERE t.timestamp >= :startDate")
//    List<TransactionEntity> findAllTransactionsAfter(
//            @Param("startDate") LocalDateTime startDate
//    );

//    // Fetch transactions where account is either sender or receiver
//    @Query("SELECT t FROM TransactionEntity t WHERE (t.fromAccount = :account OR t.toAccount = :account) AND t.timestamp >= :startDate")
//    List<TransactionEntity> findAllByAccountAndDateRange(
//            @Param("account") AccountEntity account,
//            @Param("startDate") LocalDateTime startDate
//    );
}
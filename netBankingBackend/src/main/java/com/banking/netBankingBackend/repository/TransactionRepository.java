package com.banking.netBankingBackend.repository;


import com.banking.netBankingBackend.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, RevisionRepository<TransactionEntity,Long,Integer> {
}

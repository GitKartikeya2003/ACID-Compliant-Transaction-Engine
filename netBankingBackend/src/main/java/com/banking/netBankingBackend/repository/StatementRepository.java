package com.banking.netBankingBackend.repository;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {

    List<Statement> findByAccount(AccountEntity account);
}

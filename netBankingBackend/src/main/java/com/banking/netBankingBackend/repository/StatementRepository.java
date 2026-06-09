package com.banking.netBankingBackend.repository;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    List<Statement> findByAccount(AccountEntity account);
}

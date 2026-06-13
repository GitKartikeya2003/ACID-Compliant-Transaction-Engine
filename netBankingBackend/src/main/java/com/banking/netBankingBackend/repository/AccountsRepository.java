package com.banking.netBankingBackend.repository;


import com.banking.netBankingBackend.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsRepository extends JpaRepository<AccountEntity, Long>, RevisionRepository<AccountEntity, Long, Integer> {


    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<AccountEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AccountEntity> findByAccountHash(String hashAccount);
}

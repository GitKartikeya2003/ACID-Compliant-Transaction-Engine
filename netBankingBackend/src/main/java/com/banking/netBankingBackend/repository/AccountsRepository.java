package com.banking.netBankingBackend.repository;


import com.banking.netBankingBackend.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountsRepository extends JpaRepository<AccountEntity, Long>, RevisionRepository<AccountEntity, Long, Integer> {


    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<AccountEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AccountEntity> findByAccountHash(String hashAccount);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.accountHash = :accountHash")
    Optional<AccountEntity> findByAccountHashForUpdate(@Param("accountHash") String accountHash);
}

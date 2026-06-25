package com.banking.netBankingBackend.repository;

import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert,Long> {

   // List<FraudAlert> findByAccount_Id(Long accountId);

   // List<FraudAlert> findByStatus(AlertStatus status);

   // List<FraudAlert> findByRuleType(RuleType ruleType);

   // List<FraudAlert> findByRuleTypeAndStatus(RuleType ruleType, AlertStatus status);

    //boolean existsByAccount_IdAndStatus(Long accountId, AlertStatus status);

    // For stats — counts grouped
    @Query("SELECT f.ruleType, COUNT(f) FROM FraudAlert f GROUP BY f.ruleType")
    List<Object[]> countByRuleType();

    @Query("SELECT f.ruleType, COUNT(f) FROM FraudAlert f WHERE f.status = :status GROUP BY f.ruleType")
    List<Object[]> countByRuleTypeAndStatus(@Param("status") AlertStatus status);

   long countByStatus(AlertStatus status);


    List<FraudAlert> findByAccount_AccountHash(String accountHash);


}

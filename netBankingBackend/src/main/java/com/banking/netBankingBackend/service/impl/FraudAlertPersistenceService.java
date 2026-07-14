package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.repository.FraudAlertRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAlertPersistenceService {

    private final FraudAlertRepository fraudAlertRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAlert(AccountEntity acc, RuleType ruleType, String reason) {

        // getReference() = lightweight proxy with only the PK populated.
        // No SELECT is issued on accounts, and no UPDATE will be cascaded.
        AccountEntity accountRef = entityManager.getReference(AccountEntity.class, acc.getId());

        FraudAlert alert = new FraudAlert();
        alert.setAccount(accountRef);   // FK-only proxy — safe across transaction boundaries
        alert.setRuleType(ruleType);
        alert.setStatus(AlertStatus.OPEN);
        alert.setReason(reason);
        alert.setCreatedAt(LocalDateTime.now());

        fraudAlertRepository.save(alert);
        log.info("Fraud alert created: accountId={}, rule={}, reason={}",
                acc.getId(), ruleType, reason);
    }
}
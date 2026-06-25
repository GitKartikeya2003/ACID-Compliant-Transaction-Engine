
package com.banking.netBankingBackend.service.impl;

import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.repository.FraudAlertRepository;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAlert(AccountEntity acc, RuleType ruleType, String reason) {
//        FraudAlert alert = FraudAlert.builder()
//                .account(acc)
//                .ruleType(ruleType)
//                .status(AlertStatus.OPEN)
//                .reason(reason)
//                .createdAt(LocalDateTime.now())
//                .build();

        FraudAlert alert =new FraudAlert();
        alert.setAccount(acc);
        alert.setRuleType(ruleType);
        alert.setStatus(AlertStatus.OPEN);
        alert.setReason(reason);
        alert.setCreatedAt(LocalDateTime.now());


        fraudAlertRepository.save(alert);
        log.info("Fraud alert created: account={}, rule={}, reason={}",
                acc.getAccountNumber(), ruleType, reason);
    }
}
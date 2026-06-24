package com.banking.netBankingBackend.dto.responseDtos;

import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class FraudAlertDto {
    private Long alertId;
    private String accountNumber;
    private RuleType ruleType;
    private String reason;
    private AlertStatus status;
    private LocalDateTime createdAt;
}

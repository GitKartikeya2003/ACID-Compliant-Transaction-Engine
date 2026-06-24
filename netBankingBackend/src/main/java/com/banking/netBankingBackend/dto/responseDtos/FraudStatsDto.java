package com.banking.netBankingBackend.dto.responseDtos;


import com.banking.netBankingBackend.enums.RuleType;
import lombok.Data;

import java.util.Map;

@Data
public class FraudStatsDto {

    private long totalAlerts;
    private long openAlerts;
    private long clearedAlerts;
    private long confirmedAlerts;

    private Map<RuleType, Long> countByRuleType;
    private Map<RuleType, Long> confirmedByRuleType;
}

package com.banking.netBankingBackend.mapper;

import com.banking.netBankingBackend.dto.responseDtos.FraudAlertDto;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;

public class FraudMapper {

    public static void FraudAlert_To_Dto(FraudAlert fraudAlert, FraudAlertDto fraudAlertDto) {


        fraudAlertDto.setStatus(fraudAlert.getStatus());
        fraudAlertDto.setReason(fraudAlert.getReason());
        fraudAlertDto.setAccountNumber(fraudAlert.getAccount().getAccountNumber());
        fraudAlertDto.setRuleType(fraudAlert.getRuleType());
        fraudAlertDto.setCreatedAt(fraudAlert.getCreatedAt());



    }
}

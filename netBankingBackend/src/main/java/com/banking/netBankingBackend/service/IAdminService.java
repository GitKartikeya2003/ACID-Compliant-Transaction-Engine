package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.FreezeAccountDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudAlertDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudStatsDto;

import java.util.List;

public interface IAdminService {


    List<GetBalanceDto> getAllUsers();

    List<FraudAlertDto> getAllAlerts();

    List<FraudAlertDto> alertHistory_perAccount(String accountNumber);

    FraudStatsDto getStats();

    void freezeAccount(FreezeAccountDto freezeAccountDto);

    void ClearUser(Long alertId);
}

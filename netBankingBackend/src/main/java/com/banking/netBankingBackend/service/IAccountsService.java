package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.requestDtos.SetPinDto;

import java.util.List;

public interface IAccountsService {

    void createAccount(AccountsDto accountsDto, String emailHash);

    GetBalanceDto getBalance(String accountNo);

    List<GetBalanceDto> getAllAccount(String emailHash);

    void setPin(SetPinDto setPinDto,String emailHash);
}

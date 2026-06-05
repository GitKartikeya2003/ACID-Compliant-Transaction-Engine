package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;

public interface IAccountsService {

    public void createAccount(AccountsDto accountsDto);


    GetBalanceDto getBalance(String accountNo);
}

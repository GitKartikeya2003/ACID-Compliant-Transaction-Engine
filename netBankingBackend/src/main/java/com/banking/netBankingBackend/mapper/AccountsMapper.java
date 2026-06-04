package com.banking.netBankingBackend.mapper;

import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.entity.AccountEntity;

public class AccountsMapper {

    public static void accountsDto_To_Entity(AccountEntity accountEntity,AccountsDto accountsDto,String accoutNo){

        accountEntity.setAccountNumber(accoutNo);
        accountEntity.setBalance(accountsDto.getBalance());
        accountEntity.setName(accountsDto.getName());

    }
}

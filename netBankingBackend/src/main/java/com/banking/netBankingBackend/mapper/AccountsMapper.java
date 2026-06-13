package com.banking.netBankingBackend.mapper;

import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.util.AESUtil;

public class AccountsMapper {

    public static void accountsDto_To_Entity(AccountEntity accountEntity,AccountsDto accountsDto,String accountNo){

        accountEntity.setAccountNumber(accountNo);
        accountEntity.setBalance(accountsDto.getBalance());
        accountEntity.setName(accountsDto.getName());
        accountEntity.setAccountHash(AESUtil.hash(accountNo));

    }
}

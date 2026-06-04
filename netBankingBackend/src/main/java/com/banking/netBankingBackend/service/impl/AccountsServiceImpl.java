package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.mapper.AccountsMapper;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    @Autowired
    private AccountsRepository accountsRepository;


    @Override
    public void createAccount(AccountsDto accountsDto) {

        String accountNo = generateUniqueAccountNumber();
        AccountEntity accountEntity = new AccountEntity();
        AccountsMapper.accountsDto_To_Entity(accountEntity, accountsDto, accountNo);

        accountsRepository.save(accountEntity);

    }


    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accountNumber;

        do {
            long number = (long) (random.nextDouble() * 9_000_000_000L) + 1_000_000_000L;
            accountNumber = String.valueOf(number);
        } while (accountsRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}

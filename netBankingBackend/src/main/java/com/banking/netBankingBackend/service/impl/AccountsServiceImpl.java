package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.mapper.AccountsMapper;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

    @Override
    @Cacheable(value = "accounts", key = "#accountNo")
    public GetBalanceDto getBalance(String accountNo) {

        AccountEntity account = accountsRepository.findByAccountNumber(accountNo).orElseThrow(
                () -> new ResourceNotFoundException("Account with account number " + accountNo + " not found"));

        simulateSlowDbCall();
        GetBalanceDto getBalanceDto = new GetBalanceDto();
        getBalanceDto.setBalance(account.getBalance());
        getBalanceDto.setAccountNo(account.getAccountNumber());
        getBalanceDto.setName(account.getName());

        return getBalanceDto;


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

    private void simulateSlowDbCall() {
        try {
            Thread.sleep(500);// 500ms artificial delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}

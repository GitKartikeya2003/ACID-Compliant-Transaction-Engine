package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.requestDtos.SetPinDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.exception.InvalidTransactionException;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.mapper.AccountsMapper;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.UserRepository;
import com.banking.netBankingBackend.service.IAccountsService;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class AccountsServiceImpl implements IAccountsService {

    @Autowired
    private final AccountsRepository accountsRepository;


    @Autowired
    private final UserRepository userRepository;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);



    @Override
    public void createAccount(AccountsDto accountsDto, String emailHash) {

        UserEntity user = userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accountNo = generateUniqueAccountNumber();
        AccountEntity accountEntity = new AccountEntity();
        AccountsMapper.accountsDto_To_Entity(accountEntity, accountsDto, accountNo);
        accountEntity.setUser(user);    //Don't forget to set this otherwise we cannot find the account and that account is lost forever


        accountsRepository.save(accountEntity);

    }

    @Override
    @Cacheable(value = "accounts", key = "#accountNo")
    public GetBalanceDto getBalance(String accountNo) {
        String hashEmail = AESUtil.hash(accountNo);

        AccountEntity account = accountsRepository.findByAccountHash(hashEmail).orElseThrow(
                () -> new ResourceNotFoundException("Account with account number " + accountNo + " not found"));

        // simulateSlowDbCall();
        GetBalanceDto getBalanceDto = new GetBalanceDto();
        getBalanceDto.setBalance(account.getBalance());
        getBalanceDto.setAccountNo(accountNo);
        getBalanceDto.setName(account.getName());

        return getBalanceDto;


    }

    @Override
    public List<GetBalanceDto> getAllAccount(String Hashemail) {


        UserEntity user = userRepository.findByEmailHash(Hashemail).orElseThrow(
                () -> {
                    log.error("email not found in the db");
                    return new ResourceNotFoundException("Email not found");
                }
        );

        log.info("Account Holder Name {} ", user.getFullName());


        log.info("Collecting respective Accounts for email {}", Hashemail);

        List<AccountEntity> accounts = accountsRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        log.info("Account found {}", accounts.size());

        List<GetBalanceDto> response = new ArrayList<>();
        for (AccountEntity acc : accounts) {

            String accNo = acc.getAccountNumber();
            log.info("Account No{}", accNo);

//            String decryptedAccNo = AESUtil.decrypt(encrypted_accNo);
//            log.info("Decrypted Account No{}", decryptedAccNo);


            GetBalanceDto res = new GetBalanceDto();
            res.setAccountNo(accNo);
            res.setBalance(acc.getBalance());
            res.setName(acc.getName());

            response.add(res);
        }

        log.info("Completed");


        return response;
    }


    @Override
    @Transactional
    public void setPin(SetPinDto setPinDto, String emailHash) {

        String hashAccount = AESUtil.hash(setPinDto.getAccountNumber());

        AccountEntity account = accountsRepository.findByAccountHash(hashAccount).orElseThrow(
                () -> new ResourceNotFoundException("Account number " + setPinDto.getAccountNumber() + " not found")
        );

        UserEntity user = account.getUser();

        if (user.getEmailHash().compareTo(emailHash) != 0) {

            log.warn("Account no {} is not of user: {}", account.getAccountNumber(), user.getEmail());
            throw new InvalidTransactionException(" Account No: " + account.getAccountNumber() + " does not  exists  in your ownership");
        }

        account.setTransactionPin(encoder.encode(setPinDto.getPin()));



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


//    private void simulateSlowDbCall() {
//        try {
//            Thread.sleep(500);// 500ms artificial delay
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }


}

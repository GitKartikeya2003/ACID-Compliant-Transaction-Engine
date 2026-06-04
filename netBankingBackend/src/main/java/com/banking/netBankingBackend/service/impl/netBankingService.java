package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.exception.InsufficientBalanceException;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.exception.SameAccountTransferException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.TransactionRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class netBankingService implements INetBankingService {


    private final AccountsRepository accountsRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionLogService transactionLogService;


    @Override
    @Transactional
    public void createTransaction(TransactionDto transactionDto) {

        if (transactionDto.getFrom_accountNumber()
                .equals(transactionDto.getTo_AccountNumber())) {
            throw new SameAccountTransferException("Cannot transfer to the same account");
        }


        if (transactionDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        AccountEntity fromAccount = accountsRepository.findByAccountNumber(transactionDto.getFrom_accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountEntity toAccount = accountsRepository.findByAccountNumber(transactionDto.getTo_AccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));


        if (fromAccount.getBalance().compareTo(transactionDto.getAmount()) < 0) {
            transactionLogService.saveTransaction(fromAccount, toAccount, transactionDto.getAmount(), Status.FAILED);

            throw new InsufficientBalanceException("Insufficient balance");
        } else {

            BigDecimal amountToSend = transactionDto.getAmount();

            fromAccount.setBalance(fromAccount.getBalance().subtract(amountToSend));
            toAccount.setBalance(toAccount.getBalance().add(amountToSend));

            accountsRepository.save(fromAccount);
            accountsRepository.save(toAccount);

            transactionLogService.saveTransaction(fromAccount, toAccount, amountToSend, Status.SUCCESS);


        }


    }


}

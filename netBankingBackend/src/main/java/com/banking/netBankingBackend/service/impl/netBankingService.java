package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.enums.Status;
import com.banking.netBankingBackend.exception.InsufficientBalanceException;
import com.banking.netBankingBackend.exception.InvalidTransactionException;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.exception.SameAccountTransferException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class netBankingService implements INetBankingService {


    private final AccountsRepository accountsRepository;

    private final TransactionLogService transactionLogService;


    @Override
    @Transactional(rollbackFor = Exception.class)          // Fixed import + rollbackFor
    @CacheEvict(value = "accounts", allEntries = true)
    public void createTransaction(TransactionDto transactionDto) {

        if (transactionDto.getFrom_accountNumber()
                .equals(transactionDto.getTo_AccountNumber())) {
            log.warn("Rejected transfer: same account number {}",
                    transactionDto.getFrom_accountNumber());
            throw new SameAccountTransferException("Cannot transfer to the same account");
        }


        if (transactionDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Rejected transfer: invalid amount {} from account {}",
                    transactionDto.getAmount(),
                    transactionDto.getFrom_accountNumber());
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        AccountEntity fromAccount = accountsRepository.findByAccountNumber(transactionDto.getFrom_accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        AccountEntity toAccount = accountsRepository.findByAccountNumber(transactionDto.getTo_AccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        log.info("Transfer initiated: amount={} from account={} to account={}",
                transactionDto.getAmount(),
                transactionDto.getFrom_accountNumber(),
                transactionDto.getTo_AccountNumber());


        if (fromAccount.getBalance().compareTo(transactionDto.getAmount()) < 0) {
            transactionLogService.saveTransaction(fromAccount, toAccount, transactionDto.getAmount(), Status.FAILED);
            log.warn("Transfer FAILED: insufficient balance. Account={} has={} requested={}",
                    fromAccount.getAccountNumber(),
                    fromAccount.getBalance(),
                    transactionDto.getAmount());
            throw new InsufficientBalanceException("Insufficient balance");
        } else {

            // Perform Transfer

            BigDecimal amountToSend = transactionDto.getAmount();

            fromAccount.setBalance(fromAccount.getBalance().subtract(amountToSend));
            toAccount.setBalance(toAccount.getBalance().add(amountToSend));


            transactionLogService.saveTransaction(fromAccount, toAccount, amountToSend, Status.SUCCESS);
            log.info("Transfer SUCCESS: amount={} from account={} (new balance={}) to account={} (new balance={})",
                    amountToSend,
                    fromAccount.getAccountNumber(),
                    fromAccount.getBalance(),
                    toAccount.getAccountNumber(),
                    toAccount.getBalance());

        }


    }


}

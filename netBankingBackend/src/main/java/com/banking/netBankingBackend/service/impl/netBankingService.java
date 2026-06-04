package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class netBankingService implements INetBankingService {

    @Autowired
    private AccountsRepository accountsRepository;

    @Override
    public void TransferMoney(TransactionDto transactionDto) {

    }
}

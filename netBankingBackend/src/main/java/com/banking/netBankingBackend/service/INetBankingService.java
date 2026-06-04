package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;

public interface INetBankingService {

    public void createTransaction(TransactionDto transactionDto);
}

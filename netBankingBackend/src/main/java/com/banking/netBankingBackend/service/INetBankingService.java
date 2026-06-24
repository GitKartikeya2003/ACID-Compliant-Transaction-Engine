package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;

public interface INetBankingService {

     void createTransaction(TransactionDto transactionDto,String emailHash,String pin);
}

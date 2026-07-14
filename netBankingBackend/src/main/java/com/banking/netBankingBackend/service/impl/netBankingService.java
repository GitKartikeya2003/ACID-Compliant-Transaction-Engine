package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.exception.*;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.INetBankingService;
import com.banking.netBankingBackend.service.impl.bankingImpl.TransferExecutor;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class netBankingService implements INetBankingService {

    private final AccountsRepository accountsRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final TransferExecutor transferExecutor;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);


    @Override
    public void createTransaction(TransactionDto transactionDto, String emailHash, String pin) {

        if (transactionDto.getFrom_accountNumber().equals(transactionDto.getTo_AccountNumber())) {
            log.warn("Rejected transfer: same account number {}", transactionDto.getFrom_accountNumber());
            throw new SameAccountTransferException("Cannot transfer to the same account");
        }

        if (transactionDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Rejected transfer: invalid amount {} from account {}",
                    transactionDto.getAmount(), transactionDto.getFrom_accountNumber());
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        String fromAccountNoHash = AESUtil.hash(transactionDto.getFrom_accountNumber());
        String toAccountHash = AESUtil.hash(transactionDto.getTo_AccountNumber());



        AccountEntity fromAccount = accountsRepository.findByAccountHashWithUser(fromAccountNoHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (fromAccount.getStatus() == AccountStatus.FROZEN) {
            throw new FrozenAccountException("Your Account has been frozen");
        }

        if (fromAccount.getUser().getEmailHash().compareTo(emailHash) != 0) {
            log.warn("Account no does not belong to you");
            throw new InvalidTransactionException("Account does not belong to you");
        }

        if (!accountsRepository.existsByAccountHash(toAccountHash)) {
            log.warn("To Account Not found");
            throw new ResourceNotFoundException("Account not found");
        }

        log.info("Verifying PIN code for account");
        String storedPinHash = accountsRepository.findTransactionPinByHash(fromAccountNoHash)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (storedPinHash == null) {
            throw new PinNotSetException("Transaction PIN not set for this account");
        }

        if (!encoder.matches(pin, storedPinHash)) {

            eventPublisher.publishEvent(
                    TransactionPinFailedEvent.builder()
                            .account(fromAccount)
                            .failureReason("WRONG_PIN")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            throw new InvalidPinException("Invalid transaction PIN");
        }
        transferExecutor.executeTransfer(fromAccountNoHash, toAccountHash, transactionDto.getAmount());


    }

}
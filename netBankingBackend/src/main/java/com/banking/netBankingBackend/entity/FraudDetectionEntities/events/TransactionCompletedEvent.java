package com.banking.netBankingBackend.entity.FraudDetectionEntities.events;


import com.banking.netBankingBackend.entity.AccountEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class TransactionCompletedEvent {

    private final Long fromAccountId;
    private final Long toAccountId;

    private final AccountEntity account;              // fromAccount — for fraud rule reads only
    private final BigDecimal amount;
    private final BigDecimal balanceBeforeTransfer;   // needed for account drain check
    private final LocalDateTime occurredAt;
}
package com.banking.netBankingBackend.entity.FraudDetectionEntities.events;


import com.banking.netBankingBackend.entity.AccountEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class TransactionCompletedEvent {
    private final AccountEntity account;          // full entity, not just ID
    private final BigDecimal amount;
    private final BigDecimal balanceBeforeTransfer; // needed for drain check
    private final LocalDateTime occurredAt;
}
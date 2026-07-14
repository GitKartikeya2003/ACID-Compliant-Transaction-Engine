package com.banking.netBankingBackend.entity.FraudDetectionEntities.events;

import com.banking.netBankingBackend.entity.AccountEntity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Builder
public class TransactionInsufficientFundsEvent {

    // --- IDs only — resolved to fresh references inside each async listener ---
    private final Long fromAccountId;
    private final Long toAccountId;

    // --- Read-only metadata for fraud rules ---
    private final AccountEntity account;          // fromAccount — for fraud rule reads only
    private final BigDecimal attemptedAmount;
    private final BigDecimal availableBalance;
    private final LocalDateTime timestamp;
}
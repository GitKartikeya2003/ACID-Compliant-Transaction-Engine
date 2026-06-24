package com.banking.netBankingBackend.entity.FraudDetectionEntities.events;

import com.banking.netBankingBackend.entity.AccountEntity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Builder
public class TransactionInsufficientFundsEvent {

    private final AccountEntity account;
    private final BigDecimal attemptedAmount;
    private final BigDecimal availableBalance;
    private final LocalDateTime timestamp;}
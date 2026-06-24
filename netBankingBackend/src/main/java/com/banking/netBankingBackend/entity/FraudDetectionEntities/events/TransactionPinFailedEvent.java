package com.banking.netBankingBackend.entity.FraudDetectionEntities.events;


import com.banking.netBankingBackend.entity.AccountEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class TransactionPinFailedEvent {

    private final AccountEntity account;
    private final String failureReason;
    private final LocalDateTime occurredAt;
}

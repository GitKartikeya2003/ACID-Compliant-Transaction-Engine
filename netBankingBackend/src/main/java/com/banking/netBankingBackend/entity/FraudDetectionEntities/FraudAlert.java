package com.banking.netBankingBackend.entity.FraudDetectionEntities;


import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType ruleType;


    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.OPEN;

    private LocalDateTime createdAt;

}

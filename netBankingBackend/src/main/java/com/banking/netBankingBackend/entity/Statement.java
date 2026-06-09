package com.banking.netBankingBackend.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Data
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AccountEntity account;

    private LocalDateTime generatedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private int totalTransactions;
    private BigDecimal totalAmountTransferred;
}
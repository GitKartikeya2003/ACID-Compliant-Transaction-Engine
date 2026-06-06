package com.banking.netBankingBackend.entity;


import com.banking.netBankingBackend.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; 
import org.hibernate.envers.Audited;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
@Audited
public class TransactionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private AccountEntity fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private AccountEntity toAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;     //SUCCESS /FAILED


    private LocalDateTime timestamp;





}

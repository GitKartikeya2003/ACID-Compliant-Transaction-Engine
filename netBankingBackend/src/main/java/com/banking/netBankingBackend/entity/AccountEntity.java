package com.banking.netBankingBackend.entity;

import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.util.AESAttributeConvertor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_user_id", columnList = "user_id"),           // foreign key lookup
                @Index(name = "idx_account_status", columnList = "status"),
                @Index(name = "idx_account_user_status", columnList = "user_id, status"),
                @Index(name = "idx_account_hash", columnList = "phone_account", unique = true),  // every txn/balance lookup
                @Index(name = "idx_account_number", columnList = "account_number")      // existence check on create
        }
)
@Audited
public class AccountEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    @Convert(converter = AESAttributeConvertor.class)
    private String accountNumber;

    @Column(name = "phone_account", unique = true, nullable = true)
    private String accountHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @org.hibernate.annotations.ColumnDefault("'ACTIVE'")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "transaction_pin")
    private String transactionPin;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
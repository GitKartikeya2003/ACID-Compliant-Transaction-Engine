package com.banking.netBankingBackend.entity;

import com.banking.netBankingBackend.enums.Role;
import com.banking.netBankingBackend.util.AESAttributeConvertor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_role", columnList = "role"),// for filtering the user role
                @Index(name = "idx_user_is_active", columnList = "is_active"),
        }
)
@Audited
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)        //For Credentials
    @Convert(converter = AESAttributeConvertor.class)
    private String email;

    @Column(name = "phone_number", nullable = false) //For Credentials
    @Convert(converter = AESAttributeConvertor.class)
    private String phoneNumber;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "pan_number", nullable = false)
    @Convert(converter = AESAttributeConvertor.class)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "email_hash", unique = true, nullable = true)
    private String emailHash;

    @Column(name = "phone_hash", unique = true, nullable = true)
    private String phoneHash;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
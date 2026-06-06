package com.banking.netBankingBackend.entity;

import com.banking.netBankingBackend.audit.CustomRevisionListener;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revision_info")
@RevisionEntity(CustomRevisionListener.class)
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class CustomRevisionEntity {

    @Id
    @GeneratedValue
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    private String remoteHost;
    private String remoteUser;
}


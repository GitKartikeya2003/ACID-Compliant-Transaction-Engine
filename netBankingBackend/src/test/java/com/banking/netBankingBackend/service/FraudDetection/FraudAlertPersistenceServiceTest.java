package com.banking.netBankingBackend.service.FraudDetection;



import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.repository.FraudAlertRepository;
import com.banking.netBankingBackend.service.impl.FraudAlertPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FraudAlertPersistenceServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private FraudAlertPersistenceService fraudAlertPersistenceService;

    private AccountEntity account;

    @BeforeEach
    void setUp() {
        account = new AccountEntity();
        account.setAccountNumber("ACC123456");
    }

    @Test
    @DisplayName("createAlert: saves alert with correct account")
    void createAlert_savesCorrectAccount() {
        fraudAlertPersistenceService.createAlert(account, RuleType.BRUTE_FORCE, "3 failed attempts");

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(captor.capture());

        assertThat(captor.getValue().getAccount()).isEqualTo(account);
    }

    @Test
    @DisplayName("createAlert: saves alert with correct rule type")
    void createAlert_savesCorrectRuleType() {
        fraudAlertPersistenceService.createAlert(account, RuleType.LARGE_TRANSFER, "Transfer of 200000");

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(captor.capture());

        assertThat(captor.getValue().getRuleType()).isEqualTo(RuleType.LARGE_TRANSFER);
    }

    @Test
    @DisplayName("createAlert: saves alert with OPEN status")
    void createAlert_statusIsOpen() {
        fraudAlertPersistenceService.createAlert(account, RuleType.VELOCITY, "6 transfers in 10 minutes");

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    @DisplayName("createAlert: saves alert with correct reason")
    void createAlert_savesCorrectReason() {
        String reason = "Attempted transfer drained 95% of balance";
        fraudAlertPersistenceService.createAlert(account, RuleType.ACCOUNT_DRAIN, reason);

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("createAlert: createdAt is populated")
    void createAlert_createdAtIsNotNull() {
        fraudAlertPersistenceService.createAlert(account, RuleType.BRUTE_FORCE, "reason");

        ArgumentCaptor<FraudAlert> captor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(captor.capture());

        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createAlert: repository.save is called exactly once per alert")
    void createAlert_saveCalledOnce() {
        fraudAlertPersistenceService.createAlert(account, RuleType.BRUTE_FORCE, "reason");

        verify(fraudAlertRepository).save(org.mockito.ArgumentMatchers.any(FraudAlert.class));
    }
}
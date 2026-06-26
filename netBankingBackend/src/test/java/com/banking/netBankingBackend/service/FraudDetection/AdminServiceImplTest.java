package com.banking.netBankingBackend.service.FraudDetection;



import com.banking.netBankingBackend.dto.requestDtos.FreezeAccountDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudAlertDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudStatsDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.FraudAlertRepository;
import com.banking.netBankingBackend.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private AccountEntity account;
    private FraudAlert openAlert;
    private FraudAlert clearedAlert;

    @BeforeEach
    void setUp() {
        account = new AccountEntity();
        account.setAccountNumber("ACC123456");
        account.setStatus(AccountStatus.ACTIVE);

        openAlert = FraudAlert.builder()
                .id(1L)
                .account(account)
                .ruleType(RuleType.BRUTE_FORCE)
                .status(AlertStatus.OPEN)
                .reason("3 failed PIN attempts")
                .createdAt(LocalDateTime.now())
                .build();

        clearedAlert = FraudAlert.builder()
                .id(2L)
                .account(account)
                .ruleType(RuleType.LARGE_TRANSFER)
                .status(AlertStatus.CLEARED)
                .reason("Transfer of 150000")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // getAllAlerts
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllAlerts: returns empty list when no alerts exist")
    void getAllAlerts_noAlerts_returnsEmptyList() {
        when(fraudAlertRepository.findAll()).thenReturn(Collections.emptyList());

        List<FraudAlertDto> result = adminService.getAllAlerts();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllAlerts: returns correct number of alerts")
    void getAllAlerts_twoAlerts_returnsBoth() {
        when(fraudAlertRepository.findAll()).thenReturn(List.of(openAlert, clearedAlert));

        List<FraudAlertDto> result = adminService.getAllAlerts();

        assertThat(result).hasSize(2);
    }

    // ─────────────────────────────────────────────
    // getStats
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getStats: total alert count is correct")
    void getStats_totalAlerts_correct() {
        when(fraudAlertRepository.count()).thenReturn(5L);
        when(fraudAlertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(3L);
        when(fraudAlertRepository.countByStatus(AlertStatus.CLEARED)).thenReturn(1L);
        when(fraudAlertRepository.countByStatus(AlertStatus.CONFIRMED)).thenReturn(1L);
        when(fraudAlertRepository.countByRuleType()).thenReturn(Collections.emptyList());
        when(fraudAlertRepository.countByRuleTypeAndStatus(any())).thenReturn(Collections.emptyList());

        FraudStatsDto stats = adminService.getStats();

        assertThat(stats.getTotalAlerts()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getStats: open/cleared/confirmed counts are correct")
    void getStats_statusCounts_correct() {
        when(fraudAlertRepository.count()).thenReturn(5L);
        when(fraudAlertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(3L);
        when(fraudAlertRepository.countByStatus(AlertStatus.CLEARED)).thenReturn(1L);
        when(fraudAlertRepository.countByStatus(AlertStatus.CONFIRMED)).thenReturn(1L);
        when(fraudAlertRepository.countByRuleType()).thenReturn(Collections.emptyList());
        when(fraudAlertRepository.countByRuleTypeAndStatus(any())).thenReturn(Collections.emptyList());

        FraudStatsDto stats = adminService.getStats();

        assertThat(stats.getOpenAlerts()).isEqualTo(3L);
        assertThat(stats.getClearedAlerts()).isEqualTo(1L);
        assertThat(stats.getConfirmedAlerts()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getStats: countByRuleType is populated correctly")
    void getStats_countByRuleType_correct() {
        when(fraudAlertRepository.count()).thenReturn(2L);
        when(fraudAlertRepository.countByStatus(any())).thenReturn(0L);
        when(fraudAlertRepository.countByRuleTypeAndStatus(any())).thenReturn(Collections.emptyList());
        when(fraudAlertRepository.countByRuleType()).thenReturn(List.of(
                new Object[]{RuleType.BRUTE_FORCE, 2L},
                new Object[]{RuleType.LARGE_TRANSFER, 1L}
        ));

        FraudStatsDto stats = adminService.getStats();

        assertThat(stats.getCountByRuleType()).containsEntry(RuleType.BRUTE_FORCE, 2L);
        assertThat(stats.getCountByRuleType()).containsEntry(RuleType.LARGE_TRANSFER, 1L);
    }

    // ─────────────────────────────────────────────
    // freezeAccount
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("freezeAccount: account status is set to FROZEN")
    void freezeAccount_setsStatusToFrozen() {
        // AESUtil.hash is static — mock via MockedStatic
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC123456"))
                    .thenReturn("hashed_acc");

            when(accountsRepository.findByAccountHash("hashed_acc"))
                    .thenReturn(Optional.of(account));

            FreezeAccountDto dto = new FreezeAccountDto();
            dto.setAccountNo("ACC123456");

            adminService.freezeAccount(dto);

            assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        }
    }

    @Test
    @DisplayName("freezeAccount: throws ResourceNotFoundException when account not found")
    void freezeAccount_accountNotFound_throws() {
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash(anyString()))
                    .thenReturn("nonexistent_hash");

            when(accountsRepository.findByAccountHash("nonexistent_hash"))
                    .thenReturn(Optional.empty());

            FreezeAccountDto dto = new FreezeAccountDto();
            dto.setAccountNo("INVALID");

            assertThatThrownBy(() -> adminService.freezeAccount(dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // ClearUser
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("ClearUser: alert status is set to CLEARED")
    void clearUser_setsStatusToCleared() {
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(openAlert));

        adminService.ClearUser(1L);

        assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.CLEARED);
    }

    @Test
    @DisplayName("ClearUser: throws ResourceNotFoundException when alert not found")
    void clearUser_alertNotFound_throws() {
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.ClearUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found");
    }
}

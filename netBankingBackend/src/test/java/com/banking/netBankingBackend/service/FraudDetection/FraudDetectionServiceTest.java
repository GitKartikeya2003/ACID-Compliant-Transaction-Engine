package com.banking.netBankingBackend.service.FraudDetection;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.service.impl.FraudAlertPersistenceService;
import com.banking.netBankingBackend.service.impl.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private FraudAlertPersistenceService fraudAlertPersistenceService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private AccountEntity account;

    @BeforeEach
    void setUp() {
        account = new AccountEntity();
        account.setAccountNumber("ACC123456");
        account.setBalance(new BigDecimal("10000"));

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─────────────────────────────────────────────
    // VELOCITY RULE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("Velocity: no alert fired when transfers are within limit")
    void velocity_belowLimit_noAlert() {
        when(valueOperations.increment("velocity:ACC123456")).thenReturn(3L); // under limit of 5

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("500"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.VELOCITY), anyString());
    }

    @Test
    @DisplayName("Velocity: alert fired when transfers exceed limit")
    void velocity_exceedsLimit_alertFired() {
        when(valueOperations.increment("velocity:ACC123456")).thenReturn(6L); // over limit of 5

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("500"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.VELOCITY), reasonCaptor.capture());
        assertThat(reasonCaptor.getValue()).contains("6");
    }

    @Test
    @DisplayName("Velocity: TTL is set on first transfer in window")
    void velocity_firstTransfer_ttlSet() {
        when(valueOperations.increment("velocity:ACC123456")).thenReturn(1L);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("500"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(redisTemplate).expire(eq("velocity:ACC123456"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Velocity: TTL is NOT reset on subsequent transfers")
    void velocity_subsequentTransfer_ttlNotReset() {
        when(valueOperations.increment("velocity:ACC123456")).thenReturn(3L); // not first

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("500"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    // ─────────────────────────────────────────────
    // LARGE TRANSFER RULE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("LargeTransfer: no alert fired when amount is below threshold")
    void largeTransfer_belowThreshold_noAlert() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("99999"), new BigDecimal("200000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.LARGE_TRANSFER), anyString());
    }

    @Test
    @DisplayName("LargeTransfer: alert fired when amount exceeds threshold")
    void largeTransfer_exceedsThreshold_alertFired() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("150000"), new BigDecimal("500000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.LARGE_TRANSFER), anyString());
    }

    @Test
    @DisplayName("LargeTransfer: alert fired at exactly threshold + 1")
    void largeTransfer_exactlyOneAboveThreshold_alertFired() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("100001"), new BigDecimal("500000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.LARGE_TRANSFER), anyString());
    }

    // ─────────────────────────────────────────────
    // ACCOUNT DRAIN RULE (successful transfer)
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("AccountDrain: no alert when drain ratio is below 90%")
    void accountDrain_belowThreshold_noAlert() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Transfer 80% of balance
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("8000"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.ACCOUNT_DRAIN), anyString());
    }

    @Test
    @DisplayName("AccountDrain: alert fired when drain ratio meets or exceeds 90%")
    void accountDrain_atThreshold_alertFired() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Transfer exactly 90% of balance
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("9000"), new BigDecimal("10000"), LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.ACCOUNT_DRAIN), anyString());
    }

    @Test
    @DisplayName("AccountDrain: no alert when balance before transfer is zero")
    void accountDrain_zeroBalanceBefore_noAlert() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("500"), BigDecimal.ZERO, LocalDateTime.now());

        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.ACCOUNT_DRAIN), anyString());
    }

    // ─────────────────────────────────────────────
    // ACCOUNT DRAIN RULE (insufficient funds path)
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("AccountDrain (insufficient funds): alert fired when attempted amount drains 90%+")
    void accountDrain_insufficientFunds_alertFired() {
        account.setBalance(new BigDecimal("10000"));

        TransactionInsufficientFundsEvent event = TransactionInsufficientFundsEvent.builder()
                .account(account)
                .attemptedAmount(new BigDecimal("9500"))
                .availableBalance(new BigDecimal("10000"))
                .timestamp(LocalDateTime.now())
                .build();

        fraudDetectionService.handleInsufficientFunds(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.ACCOUNT_DRAIN), anyString());
    }

    @Test
    @DisplayName("AccountDrain (insufficient funds): no alert when attempted amount is below 90%")
    void accountDrain_insufficientFunds_belowThreshold_noAlert() {
        account.setBalance(new BigDecimal("10000"));

        TransactionInsufficientFundsEvent event = TransactionInsufficientFundsEvent.builder()
                .account(account)
                .attemptedAmount(new BigDecimal("5000"))
                .availableBalance(new BigDecimal("10000"))
                .timestamp(LocalDateTime.now())
                .build();

        fraudDetectionService.handleInsufficientFunds(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.ACCOUNT_DRAIN), anyString());
    }

    // ─────────────────────────────────────────────
    // BRUTE FORCE RULE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("BruteForce: no alert when failed attempts are below limit")
    void bruteForce_belowLimit_noAlert() {
        when(valueOperations.increment("failed_attempts:ACC123456")).thenReturn(2L); // under limit of 3

        TransactionPinFailedEvent event = TransactionPinFailedEvent.builder()
                .account(account)
                .failureReason("WRONG_PIN")
                .occurredAt(LocalDateTime.now())
                .build();

        fraudDetectionService.onPinFailed(event);

        verify(fraudAlertPersistenceService, never()).createAlert(any(), eq(RuleType.BRUTE_FORCE), anyString());
    }

    @Test
    @DisplayName("BruteForce: alert fired when failed attempts reach limit")
    void bruteForce_atLimit_alertFired() {
        when(valueOperations.increment("failed_attempts:ACC123456")).thenReturn(3L); // exactly at limit

        TransactionPinFailedEvent event = TransactionPinFailedEvent.builder()
                .account(account)
                .failureReason("WRONG_PIN")
                .occurredAt(LocalDateTime.now())
                .build();

        fraudDetectionService.onPinFailed(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.BRUTE_FORCE), anyString());
    }

    @Test
    @DisplayName("BruteForce: alert fired on every attempt beyond the limit")
    void bruteForce_beyondLimit_alertFiredEachTime() {
        when(valueOperations.increment("failed_attempts:ACC123456")).thenReturn(5L);

        TransactionPinFailedEvent event = TransactionPinFailedEvent.builder()
                .account(account)
                .failureReason("WRONG_PIN")
                .occurredAt(LocalDateTime.now())
                .build();

        fraudDetectionService.onPinFailed(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.BRUTE_FORCE), anyString());
    }

    @Test
    @DisplayName("BruteForce: TTL is set on first failed attempt")
    void bruteForce_firstAttempt_ttlSet() {
        when(valueOperations.increment("failed_attempts:ACC123456")).thenReturn(1L);

        TransactionPinFailedEvent event = TransactionPinFailedEvent.builder()
                .account(account)
                .failureReason("WRONG_PIN")
                .occurredAt(LocalDateTime.now())
                .build();

        fraudDetectionService.onPinFailed(event);

        verify(redisTemplate).expire(eq("failed_attempts:ACC123456"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    // ─────────────────────────────────────────────
    // RULE ISOLATION — one rule failing doesn't stop others
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("Rule isolation: Redis failure on velocity check does not prevent large transfer check")
    void ruleIsolation_velocityFails_largeTransferStillRuns() {
        // velocity increment throws, but large transfer should still fire
        when(valueOperations.increment(startsWith("velocity:"))).thenThrow(new RuntimeException("Redis down"));

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                account, new BigDecimal("200000"), new BigDecimal("500000"), LocalDateTime.now());

        // Should NOT throw — each rule is wrapped in try/catch
        fraudDetectionService.onTransferCompleted(event);

        verify(fraudAlertPersistenceService).createAlert(eq(account), eq(RuleType.LARGE_TRANSFER), anyString());
    }
}

package com.banking.netBankingBackend.service.FraudDetection;



import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionCompletedEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionInsufficientFundsEvent;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.events.TransactionPinFailedEvent;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.exception.FrozenAccountException;
import com.banking.netBankingBackend.exception.InsufficientBalanceException;
import com.banking.netBankingBackend.exception.InvalidPinException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.impl.TransactionLogService;
import com.banking.netBankingBackend.service.impl.netBankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetBankingServiceTest {

    @Mock
    private AccountsRepository accountsRepository;

    @Mock
    private TransactionLogService transactionLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private netBankingService netBankingService;

    private AccountEntity fromAccount;
    private AccountEntity toAccount;
    private UserEntity user;
    private TransactionDto transactionDto;

    // Real encoder to hash PIN for test setup
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setEmailHash("user_email_hash");

        fromAccount = new AccountEntity();
        fromAccount.setAccountNumber("ACC_FROM");
        fromAccount.setBalance(new BigDecimal("10000"));
        fromAccount.setStatus(AccountStatus.ACTIVE);
        fromAccount.setUser(user);
        fromAccount.setTransactionPin(encoder.encode("1234")); // valid PIN

        toAccount = new AccountEntity();
        toAccount.setAccountNumber("ACC_TO");
        toAccount.setBalance(new BigDecimal("5000"));
        toAccount.setStatus(AccountStatus.ACTIVE);

        transactionDto = new TransactionDto();
        transactionDto.setFrom_accountNumber("ACC_FROM");
        transactionDto.setTo_AccountNumber("ACC_TO");
        transactionDto.setAmount(new BigDecimal("1000"));
    }

    // ─────────────────────────────────────────────
    // FROZEN ACCOUNT — SENDER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("FrozenAccount: throws FrozenAccountException when sender account is frozen")
    void createTransaction_senderFrozen_throwsFrozenAccountException() {
        fromAccount.setStatus(AccountStatus.FROZEN);

        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));

            assertThatThrownBy(() ->
                    netBankingService.createTransaction(transactionDto, "user_email_hash", "1234"))
                    .isInstanceOf(FrozenAccountException.class)
                    .hasMessageContaining("frozen");
        }
    }

    // ─────────────────────────────────────────────
    // FROZEN ACCOUNT — RECEIVER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("FrozenAccount: throws FrozenAccountException when receiver account is frozen")
    void createTransaction_receiverFrozen_throwsFrozenAccountException() {
        toAccount.setStatus(AccountStatus.FROZEN);

        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            assertThatThrownBy(() ->
                    netBankingService.createTransaction(transactionDto, "user_email_hash", "1234"))
                    .isInstanceOf(FrozenAccountException.class)
                    .hasMessageContaining("Receiver");
        }
    }

    // ─────────────────────────────────────────────
    // PIN FAILURE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("InvalidPin: throws InvalidPinException on wrong PIN")
    void createTransaction_wrongPin_throwsInvalidPinException() {
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            assertThatThrownBy(() ->
                    netBankingService.createTransaction(transactionDto, "user_email_hash", "9999"))
                    .isInstanceOf(InvalidPinException.class);
        }
    }

    @Test
    @DisplayName("InvalidPin: publishes TransactionPinFailedEvent on wrong PIN")
    void createTransaction_wrongPin_publishesPinFailedEvent() {
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            try {
                netBankingService.createTransaction(transactionDto, "user_email_hash", "9999");
            } catch (InvalidPinException ignored) {}

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(TransactionPinFailedEvent.class);

            TransactionPinFailedEvent event = (TransactionPinFailedEvent) captor.getValue();
            assertThat(event.getAccount()).isEqualTo(fromAccount);
            assertThat(event.getFailureReason()).isEqualTo("WRONG_PIN");
        }
    }

    // ─────────────────────────────────────────────
    // INSUFFICIENT BALANCE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("InsufficientBalance: throws InsufficientBalanceException")
    void createTransaction_insufficientBalance_throws() {
        transactionDto.setAmount(new BigDecimal("99999")); // more than balance of 10000

        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            assertThatThrownBy(() ->
                    netBankingService.createTransaction(transactionDto, "user_email_hash", "1234"))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    @Test
    @DisplayName("InsufficientBalance: publishes TransactionInsufficientFundsEvent")
    void createTransaction_insufficientBalance_publishesEvent() {
        transactionDto.setAmount(new BigDecimal("99999"));

        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            try {
                netBankingService.createTransaction(transactionDto, "user_email_hash", "1234");
            } catch (InsufficientBalanceException ignored) {}

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(TransactionInsufficientFundsEvent.class);
        }
    }

    // ─────────────────────────────────────────────
    // SUCCESSFUL TRANSFER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("SuccessfulTransfer: balances are updated correctly")
    void createTransaction_success_balancesUpdated() {
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            netBankingService.createTransaction(transactionDto, "user_email_hash", "1234");

            assertThat(fromAccount.getBalance()).isEqualByComparingTo("9000");
            assertThat(toAccount.getBalance()).isEqualByComparingTo("6000");
        }
    }

    @Test
    @DisplayName("SuccessfulTransfer: publishes TransactionCompletedEvent")
    void createTransaction_success_publishesCompletedEvent() {
        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");
            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_TO"))
                    .thenReturn("hashed_to");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));
            when(accountsRepository.findByAccountHash("hashed_to"))
                    .thenReturn(Optional.of(toAccount));

            netBankingService.createTransaction(transactionDto, "user_email_hash", "1234");

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(TransactionCompletedEvent.class);

            TransactionCompletedEvent event = (TransactionCompletedEvent) captor.getValue();
            assertThat(event.getAmount()).isEqualByComparingTo("1000");
            assertThat(event.getBalanceBeforeTransfer()).isEqualByComparingTo("10000");
        }
    }

    // ─────────────────────────────────────────────
    // GUARD CHECKS — checked BEFORE PIN
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("FrozenAccount: checked before PIN — no PIN event fired for frozen sender")
    void createTransaction_senderFrozen_noPinEventFired() {
        fromAccount.setStatus(AccountStatus.FROZEN);

        try (MockedStatic<com.banking.netBankingBackend.util.AESUtil> aesUtil =
                     mockStatic(com.banking.netBankingBackend.util.AESUtil.class)) {

            aesUtil.when(() -> com.banking.netBankingBackend.util.AESUtil.hash("ACC_FROM"))
                    .thenReturn("hashed_from");

            when(accountsRepository.findByAccountHash("hashed_from"))
                    .thenReturn(Optional.of(fromAccount));

            try {
                netBankingService.createTransaction(transactionDto, "user_email_hash", "9999");
            } catch (FrozenAccountException ignored) {}

            verify(eventPublisher, never()).publishEvent(any(TransactionPinFailedEvent.class));
        }
    }
}
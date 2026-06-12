package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.LoginRequestDto;
import com.banking.netBankingBackend.dto.requestDtos.UserRegistrationDto;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.enums.Role;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.exception.UserAlreadyExistsException;
import com.banking.netBankingBackend.mapper.UserMapper;
import com.banking.netBankingBackend.repository.UserRepository;
import com.banking.netBankingBackend.service.impl.UserServiceImpl;
import com.banking.netBankingBackend.service.impl.security.JwtServiceImpl;
import com.banking.netBankingBackend.util.AESUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtServiceImpl jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServiceImpl userService;

    // Static mocks need to be closed after tests to prevent memory leaks/thread issues
    private MockedStatic<AESUtil> mockedAesUtil;
    private MockedStatic<UserMapper> mockedUserMapper;

    private final String plainEmail = "test@banking.com";
    private final String hashedEmail = "hashed_test_email";
    private final String password = "securePassword123";

    @BeforeEach
    void setUp() {
        mockedAesUtil = Mockito.mockStatic(AESUtil.class);
        mockedUserMapper = Mockito.mockStatic(UserMapper.class);

        // Stubbing the common static hashing utility
        mockedAesUtil.when(() -> AESUtil.hash(plainEmail)).thenReturn(hashedEmail);
    }

    @AfterEach
    void tearDown() {
        mockedAesUtil.close();
        mockedUserMapper.close();
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================
    @Nested
    class RegisterTests {

        @Test
        void register_Success() {
            // Arrange
            UserRegistrationDto registrationDto = new UserRegistrationDto();
            registrationDto.setEmail(plainEmail);

            when(userRepository.findByEmailHash(hashedEmail)).thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> userService.register(registrationDto));

            // Verify interactions
            mockedUserMapper.verify(() -> UserMapper.userDto_to_UserEntity(any(UserEntity.class), eq(registrationDto)), times(1));
            verify(userRepository, times(1)).save(any(UserEntity.class));
        }

        @Test
        void register_UserAlreadyExists_ThrowsException() {
            // Arrange
            UserRegistrationDto registrationDto = new UserRegistrationDto();
            registrationDto.setEmail(plainEmail);

            when(userRepository.findByEmailHash(hashedEmail)).thenReturn(Optional.of(new UserEntity()));

            // Act & Assert
            assertThrows(UserAlreadyExistsException.class, () -> userService.register(registrationDto));

            // Verify execution stopped before saving
            mockedUserMapper.verifyNoInteractions();
            verify(userRepository, never()).save(any(UserEntity.class));
        }
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================
    @Nested
    class LoginTests {

        @Test
        void login_Success_ReturnsJwtToken() {
            // Arrange
            LoginRequestDto loginRequestDto = new LoginRequestDto();
            loginRequestDto.setEmail(plainEmail);
            loginRequestDto.setPassword(password);

            Authentication mockAuthentication = mock(Authentication.class);
            when(mockAuthentication.isAuthenticated()).thenReturn(true);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuthentication);

            UserEntity mockUser = new UserEntity();
            mockUser.setRole(Role.USER); // Replace with your actual Enum value if different

            when(userRepository.findByEmailHash(hashedEmail)).thenReturn(Optional.of(mockUser));
            when(jwtService.generateToken(plainEmail, Role.USER)).thenReturn("mocked-jwt-token");

            // Act
            String token = userService.login(loginRequestDto);

            // Assert
            assertEquals("mocked-jwt-token", token);
            verify(jwtService, times(1)).generateToken(plainEmail, Role.USER);
        }

        @Test
        void login_AuthenticationFails_ReturnsLoginFailedString() {
            // Arrange
            LoginRequestDto loginRequestDto = new LoginRequestDto();
            loginRequestDto.setEmail(plainEmail);
            loginRequestDto.setPassword(password);

            Authentication mockAuthentication = mock(Authentication.class);
            when(mockAuthentication.isAuthenticated()).thenReturn(false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuthentication);

            // Act
            String result = userService.login(loginRequestDto);

            // Assert
            assertEquals("login failed", result);
            verifyNoInteractions(userRepository, jwtService);
        }

        @Test
        void login_AuthenticatedButUserNotFoundInDb_ThrowsResourceNotFoundException() {
            // Arrange
            LoginRequestDto loginRequestDto = new LoginRequestDto();
            loginRequestDto.setEmail(plainEmail);
            loginRequestDto.setPassword(password);

            Authentication mockAuthentication = mock(Authentication.class);
            when(mockAuthentication.isAuthenticated()).thenReturn(true);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuthentication);

            // Simulating DB anomaly where auth manager passes, but user isn't in DB anymore
            when(userRepository.findByEmailHash(hashedEmail)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () -> userService.login(loginRequestDto));
            verifyNoInteractions(jwtService);
        }
    }
}
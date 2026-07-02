package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.LoginRequestDto;
import com.banking.netBankingBackend.dto.requestDtos.UserRegistrationDto;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.enums.Role;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.exception.UserAlreadyExistsException;
import com.banking.netBankingBackend.mapper.UserMapper;
import com.banking.netBankingBackend.repository.UserRepository;
import com.banking.netBankingBackend.service.impl.security.JwtServiceImpl;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl {

    private final UserRepository userRepository;


    private final JwtServiceImpl jwtService;
    private final AuthenticationManager authenticationManager;


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void register(UserRegistrationDto userDto) {
        try {
            String email = userDto.getEmail();


            log.info("Registration attempt initiated for email: {}", email);


            userRepository.findByEmailHash(AESUtil.hash(userDto.getEmail())).ifPresent(existingUser -> {

                log.error("Critical inconsistency:  user with email {} already exists in the database.", email);
                throw new UserAlreadyExistsException("User with email " + email + " already exists");
            });

            log.info("Registration attempt successful for email: {}", email);
            UserEntity userEntity = new UserEntity();
            UserMapper.userDto_to_UserEntity(userEntity, userDto);


            log.info("Saving in Repository for  email: {}", email);
            userRepository.save(userEntity);
            log.info("Saved in Repository for  email: {}", email);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Email, phone or PAN already registered");
        }


    }


    public String login(LoginRequestDto loginRequestDto) {

        log.info("Login attempt initiated for email: {}", loginRequestDto.getEmail());

        String emailHash = AESUtil.hash(loginRequestDto.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(AESUtil.hash(loginRequestDto.getEmail()), loginRequestDto.getPassword()));

        if (authentication.isAuthenticated()) {
            log.debug("Authentication successful for email hash. Fetching user details...");
            UserEntity userEntity = userRepository.findByEmailHash(emailHash).orElseThrow(() -> {
                log.error("Critical inconsistency: Authenticated user with email {} not found in database.", loginRequestDto.getEmail());
                return new ResourceNotFoundException("User with email " + loginRequestDto.getEmail() + " not found");
            });

            Role role = userEntity.getRole();
            log.info("User {} successfully authenticated. Generating JWT token with role: {}", loginRequestDto.getEmail(), role);

            return jwtService.generateToken(emailHash, role);

        } else {
            log.warn("Authentication failed for email: {}", loginRequestDto.getEmail());
            return "login failed";
        }


    }


}

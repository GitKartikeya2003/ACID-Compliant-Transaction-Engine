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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;


    private final JwtServiceImpl jwtService;
    private final AuthenticationManager authenticationManager;


    public void register(UserRegistrationDto userDto) {

        String email = userDto.getEmail();

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        });


        UserEntity userEntity = new UserEntity();
        UserMapper.userDto_to_UserEntity(userEntity, userDto);

        userRepository.save(userEntity);


    }


    public String login(LoginRequestDto loginRequestDto) {


        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword()));

        if (authentication.isAuthenticated()) {

            UserEntity userEntity = userRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(
                    () -> new ResourceNotFoundException("User with email " + loginRequestDto.getEmail() + " not found")
            );

            Role role = userEntity.getRole();

            return jwtService.generateToken(loginRequestDto.getEmail(), role);

        } else {
            return "login failed";
        }


    }


}

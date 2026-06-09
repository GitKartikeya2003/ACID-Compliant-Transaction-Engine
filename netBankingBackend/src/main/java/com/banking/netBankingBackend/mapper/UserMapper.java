package com.banking.netBankingBackend.mapper;

import com.banking.netBankingBackend.dto.requestDtos.UserRegistrationDto;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.enums.Role;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserMapper {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public static void userDto_to_UserEntity(UserEntity userEntity, UserRegistrationDto registerDto) {



        userEntity.setDateOfBirth(registerDto.getDateOfBirth());
        userEntity.setPassword(encoder.encode(registerDto.getPassword()));
        userEntity.setFullName(registerDto.getFullName());
        userEntity.setPhoneNumber(registerDto.getPhoneNumber());
        userEntity.setPanNumber(registerDto.getPanNumber());
        userEntity.setEmail(registerDto.getEmail());
        userEntity.setRole(Role.USER);

    }
}

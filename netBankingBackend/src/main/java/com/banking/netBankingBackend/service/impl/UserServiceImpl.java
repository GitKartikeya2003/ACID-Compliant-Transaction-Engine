package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.UserRegistrationDto;
import com.banking.netBankingBackend.entity.UserEntity;
import com.banking.netBankingBackend.exception.UserAlreadyExistsException;
import com.banking.netBankingBackend.mapper.UserMapper;
import com.banking.netBankingBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;


    public void register(UserRegistrationDto userDto) {

        String email = userDto.getEmail();

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        });


        UserEntity userEntity = new UserEntity();
        UserMapper.userDto_to_UserEntity(userEntity,userDto);

        userRepository.save(userEntity);





    }


}

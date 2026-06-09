package com.banking.netBankingBackend.dto.requestDtos;


import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRegistrationDto {


    String fullName;
    String email;
    String phoneNumber;
    String password;
    LocalDate dateOfBirth;
    String panNumber;

}

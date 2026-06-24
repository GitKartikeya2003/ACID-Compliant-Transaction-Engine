package com.banking.netBankingBackend.dto.requestDtos;


import lombok.Data;

@Data
public class SetPinDto {


    String accountNumber;
    String pin;


}

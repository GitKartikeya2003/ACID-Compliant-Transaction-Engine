package com.banking.netBankingBackend.dto.requestDtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;


@Data
public class FreezeAccountDto implements Serializable {

    @NotBlank(message = "Account cannot be empty")
    String accountNo;


}
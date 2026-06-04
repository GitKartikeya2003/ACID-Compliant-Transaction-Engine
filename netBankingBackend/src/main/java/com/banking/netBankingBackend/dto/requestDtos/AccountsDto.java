package com.banking.netBankingBackend.dto.requestDtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountsDto {



    @NotBlank(message = "Name cannot be empty")
    String name;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0",message = "Balance cannot be negative")
    BigDecimal balance;

}


//{
//  "name": "Aryan Sharma",
//  "balance": 5000.00
//}

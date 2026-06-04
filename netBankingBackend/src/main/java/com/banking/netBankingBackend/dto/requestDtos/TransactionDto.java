package com.banking.netBankingBackend.dto.requestDtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDto {

    String from_accountNumber;
    String to_AccountNumber;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0",message = "Balance cannot be negative")
    BigDecimal amount;

}

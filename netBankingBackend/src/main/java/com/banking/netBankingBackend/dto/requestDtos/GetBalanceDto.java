package com.banking.netBankingBackend.dto.requestDtos;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class GetBalanceDto implements Serializable {

    @NotBlank(message = "Account cannot be empty")
    String accountNo;

    BigDecimal balance;

    String name;

}

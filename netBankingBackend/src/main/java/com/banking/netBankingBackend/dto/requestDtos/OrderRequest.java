package com.banking.netBankingBackend.dto.requestDtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    @NotNull
    @DecimalMin(value = "1.0", message = "Minimum top-up is ₹1")
    private BigDecimal amount;


    private String currency = "INR";

    private String accountNumber;


}

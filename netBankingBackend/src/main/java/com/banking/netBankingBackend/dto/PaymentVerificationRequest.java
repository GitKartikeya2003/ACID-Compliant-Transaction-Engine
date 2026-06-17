package com.banking.netBankingBackend.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentVerificationRequest {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    //internal account to credit
    private String accountNumber;

    private BigDecimal amount;
}

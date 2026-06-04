package com.banking.netBankingBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;



@ResponseStatus(value = HttpStatus.UNPROCESSABLE_CONTENT)
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String resourceName) {

        super(String.format(resourceName, "ResourceNotFoundException"));
    }

}
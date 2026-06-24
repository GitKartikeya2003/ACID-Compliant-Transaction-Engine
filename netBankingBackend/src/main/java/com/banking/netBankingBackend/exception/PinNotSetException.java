package com.banking.netBankingBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;



@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PinNotSetException extends RuntimeException {

    public PinNotSetException(String resourceName) {

        super(String.format(resourceName, "ResourceNotFoundException"));
    }

}
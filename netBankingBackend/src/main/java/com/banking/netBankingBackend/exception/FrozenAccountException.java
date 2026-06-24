package com.banking.netBankingBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;



@ResponseStatus(value = HttpStatus.LOCKED)
public class FrozenAccountException extends RuntimeException {

    public FrozenAccountException(String message) {

        super(message);
    }

}
package com.banking.netBankingBackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class SameAccountTransferException extends RuntimeException {

    public SameAccountTransferException(String resourceName) {

        super(String.format(resourceName, "SameAccountTransferException"));
    }

}
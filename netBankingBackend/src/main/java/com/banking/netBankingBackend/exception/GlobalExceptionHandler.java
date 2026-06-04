package com.banking.netBankingBackend.exception;

import com.banking.netBankingBackend.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> GlobalExceptionHandler(Exception exception,
                                                                   WebRequest webRequest) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),   //this is to only get the api path if i would have set
                // it to true, then we will get more information that
                // is not needed right now.....
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);

    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException
                                                                                    exception,
                                                                            WebRequest webRequest) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),   //this is to only get the api path if i would have set
                // it to true, then we will get more information that
                // is not needed right now.....
                HttpStatus.CONFLICT,
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);

    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientBalanceException(InsufficientBalanceException
                                                                                    exception,
                                                                            WebRequest webRequest) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),   //this is to only get the api path if i would have set
                // it to true, then we will get more information that
                // is not needed right now.....
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);

    }


//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(DataIntegrityViolationException
//                                                                                  exception,
//                                                                          WebRequest webRequest) {
//        String message = "DataIntegrityViolationException" + exception.getMessage();
//        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
//                webRequest.getDescription(false),
//                HttpStatus.CONFLICT,
//                message,
//                LocalDateTime.now()
//        );
//
//        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);
//
//    }




    //    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(BadCredentialsException
//                                                                                    exception,
//                                                                            WebRequest webRequest) {
//        String message ="Invalid email or password: "+exception.getMessage();
//        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
//                webRequest.getDescription(false),
//                HttpStatus.UNAUTHORIZED,
//                message,
//                LocalDateTime.now()
//        );
//
//        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
//
//    }


}

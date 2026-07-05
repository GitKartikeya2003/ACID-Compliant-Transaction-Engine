package com.banking.netBankingBackend.exception;

import com.banking.netBankingBackend.dto.ErrorResponseDto;
import com.banking.netBankingBackend.dto.ResponseDto;
import org.springframework.dao.DataAccessResourceFailureException;
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

        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);

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

        return new ResponseEntity<>(errorResponseDto, HttpStatus.UNPROCESSABLE_CONTENT);

    }

    @ExceptionHandler(PinNotSetException.class)
    public ResponseEntity<ErrorResponseDto> handlePinNotSetException(PinNotSetException
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

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);

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

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleDataAccessResourceFailureException(
            DataAccessResourceFailureException exception,
            WebRequest webRequest) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database connection pool is exhausted",
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<ErrorResponseDto> handleSameAccountTransferException(SameAccountTransferException
                                                                                       exception,
                                                                               WebRequest webRequest) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),   //this is to only get the api path if i would have set
                // it to true, then we will get more information that
                // is not needed right now.....
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);

    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<String> handleInvalidTransaction(InvalidTransactionException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<String> handleInvalidPinException(InvalidPinException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException
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

        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);

    }

    @ExceptionHandler(FrozenAccountException.class)
    public ResponseEntity<ResponseDto> handleAccountFrozen(FrozenAccountException ex) {
        return ResponseEntity
                .status(HttpStatus.LOCKED)  // 423
                .body(new ResponseDto("423", ex.getMessage()));
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

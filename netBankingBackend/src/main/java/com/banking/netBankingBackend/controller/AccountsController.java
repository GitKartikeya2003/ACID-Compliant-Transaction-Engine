package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.ResponseDto;
import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.service.IAccountsService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@AllArgsConstructor
public class AccountsController {

    @Autowired
    private IAccountsService accountsService;


    @PostMapping("/createAccount")
    public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody AccountsDto accountsDto) {

        accountsService.createAccount(accountsDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDto("201", "Account created successfully"));

    }

    @PostMapping("/transaction")
    public ResponseEntity<ResponseDto> createTransaction(@Valid @RequestBody TransactionDto transactionDto) {


        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200", "Transaction  successful"));

    }






}

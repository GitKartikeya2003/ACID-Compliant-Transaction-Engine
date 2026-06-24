package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.requestDtos.AccountsDto;
import com.banking.netBankingBackend.dto.ResponseDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.requestDtos.SetPinDto;
import com.banking.netBankingBackend.dto.requestDtos.TransactionDto;
import com.banking.netBankingBackend.service.IAccountsService;
import com.banking.netBankingBackend.service.INetBankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


@Controller
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
public class AccountsController {


    private final IAccountsService accountsService;

    private final INetBankingService netBankingService;


    @PostMapping("/createAccount")
    public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody AccountsDto accountsDto) {
        String emailHash = Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getName();
        accountsService.createAccount(accountsDto, emailHash);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDto("201", "Account created successfully"));

    }

    @GetMapping("/get-balance")
    public ResponseEntity<GetBalanceDto> getBalance(@Valid @RequestParam String accountNo) {

        GetBalanceDto details = accountsService.getBalance(accountNo);
        return ResponseEntity.status(HttpStatus.OK).body(details);


    }


    @PostMapping("/transaction")
    public ResponseEntity<ResponseDto> createTransaction(@Valid @RequestBody TransactionDto transactionDto,@RequestHeader("X-transaction-Pin") String pin) {

        String emailHash = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();


        netBankingService.createTransaction(transactionDto, emailHash,pin);
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200", "Transaction  successful"));

    }


    @GetMapping("/fetch-all-accounts")
    public ResponseEntity<List<GetBalanceDto>> fetchAllAccounts() {

        String emailHash = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        List<GetBalanceDto> response = accountsService.getAllAccount(emailHash);

        return ResponseEntity.status(HttpStatus.OK).body(response);


    }


    @PostMapping("/set-pin")
    public ResponseEntity<ResponseDto> setPin(@RequestBody SetPinDto setPinDto) {

        String emailHash = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        accountsService.setPin(setPinDto,emailHash);

        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200", "Pin successfully set"));

    }

}

package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.ResponseDto;
import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudAlertDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudStatsDto;
import com.banking.netBankingBackend.service.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/admin", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
public class AdminController {


    private final IAdminService adminService;


    @GetMapping("/fetch-all-users")
    public ResponseEntity<List<GetBalanceDto>> fetchAllUsers() {


        List<GetBalanceDto> response = adminService.getAllUsers();


        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    //------------------------------ Fraud Detection ----------------------------------

    @GetMapping("/fraud/alerts")
    public ResponseEntity<List<FraudAlertDto>> allFraudAlerts() {


        List<FraudAlertDto> frauds = adminService.getAllAlerts();

        return ResponseEntity.status(HttpStatus.OK).body(frauds);
    }


    @GetMapping("/fraud/alerts-user")
    public ResponseEntity<List<FraudAlertDto>> alertHistory_perAccount(@RequestParam String accountNumber) {

        List<FraudAlertDto> response = adminService.alertHistory_perAccount(accountNumber);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PatchMapping("/fraud/alerts")
    public ResponseEntity<ResponseDto> ClearUser(@RequestBody FraudAlertDto fraudAlertDto) {


        adminService.ClearUser(fraudAlertDto);

        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200","Account cleared Successfully"));

    }

    @PostMapping("/fraud/freeze")
    public ResponseEntity<ResponseDto> freezeAccount(@RequestBody GetBalanceDto getBalanceDto ) {

        adminService.freezeAccount(getBalanceDto);

        return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200","Account Freezed Successfully"));


    }

    @GetMapping("/fraud/stats")
    public ResponseEntity<FraudStatsDto> getStats() {
        FraudStatsDto stats = adminService.getStats();
        return ResponseEntity.status(HttpStatus.OK).body(stats);
    }


//    GET /api/admin/fraud/alerts — list all alerts, filterable by rule type and status
//    GET /api/admin/fraud/alerts/{accountId} — alert history for one account
//    PATCH /api/admin/fraud/alerts/{alertId} — admin marks it cleared (false positive) or confirmed
//    POST /api/admin/accounts/{id}/freeze — actually act on a confirmed alert
//    GET /api/admin/fraud/stats — counts grouped by rule type, for the dashboard summary view
}

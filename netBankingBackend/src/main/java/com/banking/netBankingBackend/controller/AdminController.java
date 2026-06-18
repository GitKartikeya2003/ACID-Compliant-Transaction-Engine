package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.service.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/admin", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
public class AdminController {


    private final IAdminService adminService;


    @GetMapping("/fetch-all-users")
    public ResponseEntity<List<GetBalanceDto>> fetchAllUsers(){


        List<GetBalanceDto> response = adminService.getAllUsers();


        return ResponseEntity.status(HttpStatus.OK).body(response);




    }

//    GET /api/admin/fraud/alerts — list all alerts, filterable by rule type and status
//    GET /api/admin/fraud/alerts/{accountId} — alert history for one account
//    PATCH /api/admin/fraud/alerts/{alertId} — admin marks it cleared (false positive) or confirmed
//    POST /api/admin/accounts/{id}/freeze — actually act on a confirmed alert
//    GET /api/admin/fraud/stats — counts grouped by rule type, for the dashboard summary view
}

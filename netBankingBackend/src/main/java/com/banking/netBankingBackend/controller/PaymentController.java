package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.PaymentVerificationRequest;
import com.banking.netBankingBackend.dto.requestDtos.OrderRequest;
import com.banking.netBankingBackend.service.impl.RazorPayService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/api/payments", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
public class PaymentController {

    private final RazorPayService razorPayService;


    // Step 1: Create Razorpay order
    @PostMapping("/topup")
    public ResponseEntity<String> createTopUp(
            @Valid @RequestBody OrderRequest orderRequest) throws RazorpayException {

        String order = razorPayService.createOrder(orderRequest);
        return ResponseEntity.ok(order);
    }

    // Step 2: Frontend callback after payment success
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestBody PaymentVerificationRequest req) throws RazorpayException {

        String result = razorPayService.verifyAndCredit(req);
        return ResponseEntity.ok(result);
    }

}

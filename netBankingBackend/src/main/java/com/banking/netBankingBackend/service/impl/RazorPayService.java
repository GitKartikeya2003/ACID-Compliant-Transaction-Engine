package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.PaymentVerificationRequest;
import com.banking.netBankingBackend.dto.requestDtos.OrderRequest;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.util.AESUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorPayService {


    @Value("${razorpay.key-id}")
    private String apiKey;

    @Value("${razorpay.key-secret}")
    private String apiSecret;

    @Value("${razorpay.verify-signature:true}")
    private boolean verifySign;

    @PostConstruct
    public void debugKeys() {
        log.info("RAZORPAY apiKey = [{}]", apiKey);
        log.info("RAZORPAY apiSecret length = [{}]", apiSecret != null ? apiSecret.length() : "NULL");
    }


    private final AccountsRepository accountsRepository;


    public String createOrder(OrderRequest orderRequestDto) throws RazorpayException {

        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        JSONObject orderJson = new JSONObject();


        orderJson.put("amount", orderRequestDto.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue());
        orderJson.put("currency", orderRequestDto.getCurrency());
        orderJson.put("receipt", "txn_" + System.currentTimeMillis());
        orderJson.put("payment_capture", 1);


        Order order = razorpayClient.orders.create(orderJson);

        log.info("Razorpay order created: id={} amount={}",
                order.get("id"), order.get("amount"));

        return order.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", allEntries = true)
    public String verifyAndCredit(PaymentVerificationRequest req) throws RazorpayException {


        if (verifySign) {
            boolean isValid = verifySignature(
                    req.getRazorpayOrderId(),
                    req.getRazorpayPaymentId(),
                    req.getRazorpaySignature()
            );

            if (!isValid) {
                log.warn("Invalid Razorpay signature for orderId={}", req.getRazorpayOrderId());
                throw new SecurityException("Payment signature verification failed");
            }
        }


        String accountHash = AESUtil.hash(req.getAccountNumber());

        AccountEntity account = accountsRepository.findByAccountHash(accountHash).orElseThrow(
                () -> {
                    log.warn("Account does not exists");
                    return new ResourceNotFoundException("Account Not found");
                }
        );

        //BigDecimal creditAmount = fetchPaymentAmount(req.getRazorpayPaymentId());
        BigDecimal creditAmount;

        if (!verifySign && req.getAmount() != null) {
            creditAmount = req.getAmount();
            log.info("Test mode: crediting ₹{} without Razorpay fetch", creditAmount);
        } else {
            creditAmount = fetchPaymentAmount(req.getRazorpayPaymentId());
        }

        account.setBalance(account.getBalance().add(creditAmount));
        log.info("Topped up account={} by ₹{} via Razorpay paymentId={}",
                account.getAccountNumber(), creditAmount, req.getRazorpayPaymentId());

        return "Top-up of ₹" + creditAmount + " successful!";


    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String computed = hmacSHA256(payload, apiSecret);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }


    private String hmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private BigDecimal fetchPaymentAmount(String paymentId) {
        try {
            RazorpayClient client = new RazorpayClient(apiKey, apiSecret);
            com.razorpay.Payment payment = client.payments.fetch(paymentId);
            // amount is in paise
            long amountInPaise = payment.get("amount");
            return BigDecimal.valueOf(amountInPaise).divide(BigDecimal.valueOf(100));
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment amount for paymentId={}", paymentId, e);
            throw new RuntimeException("Could not verify payment amount", e);
        }
    }
}

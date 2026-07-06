package com.banking.netBankingBackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stand-in for a real metrics system. Counts fraud-rule failures in memory
 * and logs a summary periodically, so failures are visible somewhere other
 * than a log line nobody greps. Replace with MeterRegistry.counter(...) calls
 * once Micrometer/Prometheus/whatever is actually wired into the project —
 * this class exists so the fraud rewrite compiles and fails LOUDLY today
 * without forcing a metrics-stack decision under deadline pressure.
 */
@Slf4j
@Component
public class FraudFailureCounter {

    private final ConcurrentHashMap<String, AtomicLong> failuresByRule = new ConcurrentHashMap<>();

    public void increment(String ruleName) {
        failuresByRule.computeIfAbsent(ruleName, k -> new AtomicLong()).incrementAndGet();
    }

    // Every 5 minutes, dump non-zero counters at WARN so they're impossible
    // to miss in log aggregation, even without a dashboard.
    @Scheduled(fixedRate = 300_000)
    public void logSummary() {
        failuresByRule.forEach((rule, count) -> {
            long value = count.get();
            if (value > 0) {
                log.warn("Fraud rule '{}' has failed {} time(s) since last report", rule, value);
            }
        });
    }
}
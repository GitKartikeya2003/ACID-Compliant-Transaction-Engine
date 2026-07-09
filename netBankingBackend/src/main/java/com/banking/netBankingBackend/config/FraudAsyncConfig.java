package com.banking.netBankingBackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling // required for FraudFailureCounter's periodic summary log
public class FraudAsyncConfig {

    @Bean("fraudExecutor")
    public Executor fraudExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);  // was 5 — sized for 100 concurrent users each firing fraud checks
        executor.setMaxPoolSize(20);   // was 10 — prevents CallerRunsPolicy fallback under burst load
        executor.setQueueCapacity(1000); // was 500 — deeper buffer for high-throughput windows
        executor.setThreadNamePrefix("fraud-async-");
        // Under sustained overload, run the fraud check on the calling thread
        // instead of silently dropping it. This trades a little latency for
        // never losing a fraud check outright.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated pool for async transaction log writes (SUCCESS + FAILED).
     * Kept separate from fraudExecutor so a burst of fraud alerts never
     * starves transaction logging I/O, and vice versa.
     */
    @Bean("txnLogExecutor")
    public Executor txnLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   // enough for 100 concurrent users each triggering a log write
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("txn-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

/*
 SEPARATE small pool for fraud writes so they
 can never compete with transfer-path connections for the main 50-connection pool.
 Requires a second DataSource bean (@Qualifier("fraudDataSource")) wired into
 a fraud-specific EntityManagerFactory/JdbcTemplate if FraudAlertRepository
 needs true isolation. If that's more plumbing than you want right now, the
 async move alone (this file + the two rewritten service files) already
 removes fraud writes from the transfer request thread, which is the
 majority of the risk. Full datasource isolation is the next hardening step,
 not a blocker to shipping the async fix.

 */
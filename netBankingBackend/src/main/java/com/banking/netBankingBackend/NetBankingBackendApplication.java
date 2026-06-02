package com.banking.netBankingBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class NetBankingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NetBankingBackendApplication.class, args);
	}

}

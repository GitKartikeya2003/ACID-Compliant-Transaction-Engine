package com.banking.netBankingBackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.TimeZone;

@SpringBootTest
class NetBankingBackendApplicationTests {

	@Test
	void contextLoads() {
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(NetBankingBackendApplication.class, args);



	}

}

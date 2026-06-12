package com.banking.netBankingBackend.service;


import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class AESKeyGeneratorTest {

    @Test
    void generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();
        String keyString = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("YOUR AES SECRET KEY (copy this):");
        System.out.println(keyString);
    }
}
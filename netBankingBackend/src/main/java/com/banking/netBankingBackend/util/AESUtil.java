package com.banking.netBankingBackend.util;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AESUtil {


    @Value("${aes.secret.key}")
    private String aesKey;

    @Value("${hmac.secret.key}")   // separate key from your AES key
    private String hmacKey;

    private static String STATIC_HMAC_KEY;


    private static String STATIC_KEY;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12byte = 96 bits NIST reccommends 96 bit IV for GCM
    // for best performance and Security combination
    private static final int GCM_TAG_LENGTH = 128;


    @PostConstruct
    public void init() {
        STATIC_KEY = aesKey;
        STATIC_HMAC_KEY = hmacKey;
    }


    private static GCMParameterSpec generateIv(byte[] iv) { //only encrypt() calls this

        new SecureRandom().nextBytes(iv);
        return new GCMParameterSpec(GCM_TAG_LENGTH, iv);

    }


    public static String encrypt(String plainText) {

        try {


            byte[] keyBytes = Base64.getDecoder().decode(STATIC_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            //generate fresh random IV for every encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            GCMParameterSpec gcmSpec = generateIv(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to encrypted bytes (IV + ciphertext + GCM tag)
            // GCM automatically appends the auth tag to encrypted bytes
            byte[] combined = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);


        } catch (Exception e) {
            throw new RuntimeException("Error encrypting the value" + e);
        }
    }


    public static String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extract IV from first 12 bytes
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            // Rest is ciphertext + GCM auth tag
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            byte[] keyBytes = Base64.getDecoder().decode(STATIC_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // If data was tampered, this line throws AEADBadTagException
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }


    public static String hash(String plainText) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    STATIC_HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing value", e);
        }
    }

}

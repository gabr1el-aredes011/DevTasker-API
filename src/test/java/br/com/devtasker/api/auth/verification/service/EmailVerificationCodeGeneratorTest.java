package br.com.devtasker.api.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;

class EmailVerificationCodeGeneratorTest {

    @Test
    void shouldGenerateSixNumericCharacters() {
        EmailVerificationProperties properties =
                new EmailVerificationProperties(
                        6,
                        10,
                        5,
                        60,
                        validSecret()
                );

        EmailVerificationCodeGenerator generator =
                new EmailVerificationCodeGenerator(
                        new SecureRandom(),
                        properties
                );

        for (
                int execution = 0;
                execution < 100;
                execution++
        ) {
            String code = generator.generate();

            assertEquals(
                    6,
                    code.length()
            );

            assertTrue(
                    code.matches("\\d{6}")
            );
        }
    }

    private static String validSecret() {
        byte[] secretBytes = new byte[32];

        return Base64
                .getEncoder()
                .encodeToString(secretBytes);
    }
}
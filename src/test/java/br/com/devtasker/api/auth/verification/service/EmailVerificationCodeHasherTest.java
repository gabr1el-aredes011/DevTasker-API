package br.com.devtasker.api.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;

class EmailVerificationCodeHasherTest {

    private final EmailVerificationCodeHasher hasher =
            new EmailVerificationCodeHasher(
                    properties()
            );

    @Test
    void shouldMatchCorrectCodeForSameUser() {
        Long userId = 15L;
        String rawCode = "483921";

        String hash =
                hasher.hash(
                        userId,
                        rawCode
                );

        assertTrue(
                hasher.matches(
                        userId,
                        rawCode,
                        hash
                )
        );
    }

    @Test
    void shouldRejectIncorrectCode() {
        Long userId = 15L;

        String hash =
                hasher.hash(
                        userId,
                        "483921"
                );

        assertFalse(
                hasher.matches(
                        userId,
                        "111111",
                        hash
                )
        );
    }

    @Test
    void shouldBindCodeToSpecificUser() {
        String rawCode = "483921";

        String firstUserHash =
                hasher.hash(
                        15L,
                        rawCode
                );

        assertFalse(
                hasher.matches(
                        99L,
                        rawCode,
                        firstUserHash
                )
        );
    }

    private static EmailVerificationProperties
            properties() {

        byte[] secretBytes = new byte[32];

        for (
                int index = 0;
                index < secretBytes.length;
                index++
        ) {
            secretBytes[index] =
                    (byte) (index + 1);
        }

        String encodedSecret =
                Base64
                        .getEncoder()
                        .encodeToString(secretBytes);

        return new EmailVerificationProperties(
                6,
                10,
                5,
                60,
                encodedSecret
        );
    }
}
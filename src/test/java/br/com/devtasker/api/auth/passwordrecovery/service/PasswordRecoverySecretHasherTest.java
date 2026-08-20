package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.devtasker.api.auth.passwordrecovery.config.PasswordRecoveryProperties;

class PasswordRecoverySecretHasherTest {

    private PasswordRecoverySecretHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordRecoverySecretHasher(properties());
    }

    @Test
    void shouldMatchCodeOnlyForTheSameUserAndValue() {
        String storedHash = hasher.hashCode(7L, "123456");

        assertTrue(hasher.codeMatches(7L, "123456", storedHash));
        assertFalse(hasher.codeMatches(7L, "654321", storedHash));
        assertFalse(hasher.codeMatches(8L, "123456", storedHash));
    }

    @Test
    void shouldUseSeparatePurposesForCodeAndResetToken() {
        String codeHash = hasher.hashCode(7L, "123456");
        String tokenHash = hasher.hashResetToken("123456");

        assertNotEquals(codeHash, tokenHash);
    }

    private static PasswordRecoveryProperties properties() {
        return new PasswordRecoveryProperties(
                6,
                10,
                5,
                60,
                10,
                Base64.getEncoder().encodeToString(new byte[32])
        );
    }
}

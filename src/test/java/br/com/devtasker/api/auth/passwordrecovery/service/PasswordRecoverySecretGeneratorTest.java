package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.auth.passwordrecovery.config.PasswordRecoveryProperties;

class PasswordRecoverySecretGeneratorTest {

    @Test
    void shouldGenerateSixNumericDigitsAndThirtyTwoTokenBytes() {
        PasswordRecoverySecretGenerator generator =
                new PasswordRecoverySecretGenerator(properties());

        String code = generator.generateCode();
        String resetToken = generator.generateResetToken();

        assertEquals(6, code.length());
        assertTrue(code.chars().allMatch(Character::isDigit));
        assertEquals(
                32,
                Base64.getUrlDecoder().decode(resetToken).length
        );
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

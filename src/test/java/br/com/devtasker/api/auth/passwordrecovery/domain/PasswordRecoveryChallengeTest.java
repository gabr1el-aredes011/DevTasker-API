package br.com.devtasker.api.auth.passwordrecovery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.user.domain.UserAccount;

class PasswordRecoveryChallengeTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(
            2026,
            8,
            20,
            15,
            0,
            0,
            0,
            ZoneOffset.UTC
    );

    @Test
    void shouldRenewCodeAndInvalidatePreviousAuthorization() {
        PasswordRecoveryChallenge challenge = newChallenge();

        challenge.registerFailedAttempt();
        challenge.authorizeReset(
                "token-hash",
                NOW.plusMinutes(10),
                NOW
        );

        challenge.renewCode(
                "new-code-hash",
                NOW.plusMinutes(20),
                NOW.plusMinutes(1)
        );

        assertEquals("new-code-hash", challenge.getCodeHash());
        assertEquals(0, challenge.getAttemptCount());
        assertNull(challenge.getVerifiedAt());
        assertNull(challenge.getResetTokenHash());
        assertNull(challenge.getResetTokenExpiresAt());
    }

    @Test
    void shouldApplyExpirationAttemptAndCooldownBoundaries() {
        PasswordRecoveryChallenge challenge = newChallenge();

        assertFalse(challenge.isCodeExpiredAt(NOW.plusMinutes(9)));
        assertTrue(challenge.isCodeExpiredAt(NOW.plusMinutes(10)));
        assertFalse(
                challenge.canBeResentAt(NOW.plusSeconds(59), Duration.ofSeconds(60))
        );
        assertTrue(
                challenge.canBeResentAt(NOW.plusSeconds(60), Duration.ofSeconds(60))
        );

        for (int attempt = 0; attempt < 5; attempt++) {
            challenge.registerFailedAttempt();
        }

        assertTrue(challenge.hasReachedAttemptLimit(5));
    }

    @Test
    void shouldOnlyAcceptResetTokenBeforeItsExpiration() {
        PasswordRecoveryChallenge challenge = newChallenge();
        challenge.authorizeReset(
                "token-hash",
                NOW.plusMinutes(10),
                NOW
        );

        assertTrue(challenge.hasActiveResetTokenAt(NOW.plusMinutes(9)));
        assertFalse(challenge.hasActiveResetTokenAt(NOW.plusMinutes(10)));
    }

    private static PasswordRecoveryChallenge newChallenge() {
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(7L);

        return PasswordRecoveryChallenge.create(
                user,
                UUID.randomUUID(),
                "code-hash",
                NOW.plusMinutes(10),
                NOW
        );
    }
}

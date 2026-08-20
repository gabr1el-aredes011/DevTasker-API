package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.devtasker.api.auth.passwordrecovery.config.PasswordRecoveryProperties;
import br.com.devtasker.api.auth.passwordrecovery.domain.PasswordRecoveryChallenge;
import br.com.devtasker.api.auth.passwordrecovery.dto.PasswordRecoveryChallengeResponse;
import br.com.devtasker.api.auth.passwordrecovery.dto.VerifyPasswordRecoveryResponse;
import br.com.devtasker.api.auth.passwordrecovery.repository.PasswordRecoveryChallengeRepository;
import br.com.devtasker.api.auth.passwordrecovery.repository.PasswordRecoveryUserLockRepository;
import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.exception.PasswordRecoveryException;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    private static final String CLIENT = "client-fingerprint";

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

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordRecoveryUserLockRepository userLockRepository;

    @Mock
    private PasswordRecoveryChallengeRepository challengeRepository;

    @Mock
    private PasswordRecoverySecretGenerator secretGenerator;

    @Mock
    private PasswordRecoverySecretHasher secretHasher;

    @Mock
    private PasswordRecoveryEmailDispatcher emailDispatcher;

    @Mock
    private PasswordRecoveryRateLimiter rateLimiter;

    @Mock
    private PasswordRecoveryFingerprintService fingerprintService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        PasswordRecoveryProperties properties =
                new PasswordRecoveryProperties(
                        6,
                        10,
                        5,
                        60,
                        10,
                        Base64.getEncoder().encodeToString(new byte[32])
                );

        Clock clock = Clock.fixed(
                Instant.parse("2026-08-20T15:00:00Z"),
                ZoneOffset.UTC
        );

        lenient().when(
                rateLimiter.allow(
                        anyString(),
                        anyString(),
                        anyInt(),
                        any(Duration.class)
                )
        ).thenReturn(true);

        lenient().when(
                fingerprintService.identifierFingerprint(
                        anyString(),
                        anyString()
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
                        + ":"
                        + invocation.getArgument(1)
        );

        service = new PasswordRecoveryService(
                userAccountRepository,
                userLockRepository,
                challengeRepository,
                secretGenerator,
                secretHasher,
                emailDispatcher,
                rateLimiter,
                fingerprintService,
                passwordEncoder,
                properties,
                clock
        );
    }

    @Test
    void shouldReturnFreshIsomorphicChallengesForUnknownAndUnverifiedAccounts() {
        UserAccount unverifiedUser = user(false);
        when(userLockRepository.findByEmailForUpdate("unknown@devtasker.test"))
                .thenReturn(Optional.empty());
        when(userLockRepository.findByEmailForUpdate("pending@devtasker.test"))
                .thenReturn(Optional.of(unverifiedUser));

        PasswordRecoveryChallengeResponse unknown = service.request(
                "unknown@devtasker.test",
                CLIENT
        );
        PasswordRecoveryChallengeResponse unverified = service.request(
                "pending@devtasker.test",
                CLIENT
        );

        assertNotEquals(unknown.challengeId(), unverified.challengeId());
        assertEquals(NOW.plusMinutes(10), unknown.expiresAt());
        assertEquals(unknown.expiresAt(), unverified.expiresAt());
        assertEquals(NOW.plusSeconds(60), unknown.resendAvailableAt());
        assertEquals(
                unknown.resendAvailableAt(),
                unverified.resendAvailableAt()
        );
        verifyNoInteractions(challengeRepository, emailDispatcher);
    }

    @Test
    void shouldAcceptSemanticallyInvalidRequestWithoutDatabaseLookup() {
        PasswordRecoveryChallengeResponse response = service.request(
                "not-an-email",
                CLIENT
        );

        assertEquals(NOW.plusMinutes(10), response.expiresAt());
        verifyNoInteractions(
                userLockRepository,
                challengeRepository,
                emailDispatcher
        );
    }

    @Test
    void shouldPersistAndScheduleChallengeForVerifiedAccount() {
        UserAccount user = user(true);
        prepareVerifiedAccount(user);
        when(challengeRepository.findByUserIdForUpdate(7L))
                .thenReturn(Optional.empty());
        when(secretGenerator.generateCode()).thenReturn("123456");
        when(secretHasher.hashCode(7L, "123456"))
                .thenReturn("code-hash");

        PasswordRecoveryChallengeResponse response = service.request(
                " USER@DEVTASKER.TEST ",
                CLIENT
        );

        ArgumentCaptor<PasswordRecoveryChallenge> challengeCaptor =
                ArgumentCaptor.forClass(PasswordRecoveryChallenge.class);
        verify(challengeRepository).saveAndFlush(challengeCaptor.capture());

        PasswordRecoveryChallenge challenge = challengeCaptor.getValue();
        assertEquals(response.challengeId(), challenge.getPublicId());
        assertEquals("code-hash", challenge.getCodeHash());
        assertEquals(NOW.plusMinutes(10), challenge.getExpiresAt());
        verify(emailDispatcher).dispatchAfterCommit(
                any(PasswordRecoveryEmailMessage.class)
        );
    }

    @Test
    void shouldReturnDecoyDuringCooldownWithoutMutatingRealChallenge() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge =
                org.mockito.Mockito.mock(PasswordRecoveryChallenge.class);
        UUID realId = UUID.randomUUID();
        prepareVerifiedAccount(user);
        when(challengeRepository.findByUserIdForUpdate(7L))
                .thenReturn(Optional.of(challenge));
        when(challenge.hasActiveResetTokenAt(NOW)).thenReturn(false);
        when(challenge.canBeResentAt(NOW, Duration.ofSeconds(60)))
                .thenReturn(false);

        PasswordRecoveryChallengeResponse response = service.request(
                "user@devtasker.test",
                CLIENT
        );

        assertNotEquals(realId, response.challengeId());
        verify(challengeRepository, never()).save(any());
        verifyNoInteractions(secretGenerator, secretHasher, emailDispatcher);
    }

    @Test
    void shouldPreserveActiveResetTokenOnNewAnonymousRequest() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge =
                org.mockito.Mockito.mock(PasswordRecoveryChallenge.class);
        prepareVerifiedAccount(user);
        when(challengeRepository.findByUserIdForUpdate(7L))
                .thenReturn(Optional.of(challenge));
        when(challenge.hasActiveResetTokenAt(NOW)).thenReturn(true);

        service.request("user@devtasker.test", CLIENT);

        verify(challenge, never()).restart(any(), any(), any(), any());
        verify(challengeRepository, never()).save(any());
        verifyNoInteractions(secretGenerator, secretHasher, emailDispatcher);
    }

    @Test
    void shouldKeepResponseNeutralWhenEmailSchedulingFails() {
        UserAccount user = user(true);
        prepareVerifiedAccount(user);
        when(challengeRepository.findByUserIdForUpdate(7L))
                .thenReturn(Optional.empty());
        when(secretGenerator.generateCode()).thenReturn("123456");
        when(secretHasher.hashCode(7L, "123456"))
                .thenReturn("code-hash");
        doThrow(new IllegalStateException("rejected"))
                .when(emailDispatcher)
                .dispatchAfterCommit(any(PasswordRecoveryEmailMessage.class));

        PasswordRecoveryChallengeResponse response = service.request(
                "user@devtasker.test",
                CLIENT
        );

        assertEquals(NOW.plusMinutes(10), response.expiresAt());
        verify(challengeRepository).saveAndFlush(any());
    }

    @Test
    void shouldResendAfterCooldownUsingUserThenChallengeLockOrder() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge =
                org.mockito.Mockito.mock(PasswordRecoveryChallenge.class);
        UUID challengeId = UUID.randomUUID();
        prepareChallengeLookup(challengeId, user, challenge);
        when(challenge.hasActiveResetTokenAt(NOW)).thenReturn(false);
        when(challenge.canBeResentAt(NOW, Duration.ofSeconds(60)))
                .thenReturn(true);
        when(secretGenerator.generateCode()).thenReturn("654321");
        when(secretHasher.hashCode(7L, "654321"))
                .thenReturn("new-code-hash");

        service.resend(challengeId.toString(), CLIENT);

        verify(challenge).renewCode(
                "new-code-hash",
                NOW.plusMinutes(10),
                NOW
        );
        verify(challengeRepository).saveAndFlush(challenge);
        verify(emailDispatcher).dispatchAfterCommit(
                any(PasswordRecoveryEmailMessage.class)
        );
    }

    @Test
    void shouldRegisterFailedAttemptAndReturnOnlyGenericError() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge = activeChallenge(user);
        UUID challengeId = UUID.randomUUID();
        prepareChallengeLookup(challengeId, user, challenge);
        when(secretHasher.codeMatches(7L, "123456", "code-hash"))
                .thenReturn(false);

        PasswordRecoveryException exception = assertThrows(
                PasswordRecoveryException.class,
                () -> service.verify(
                        challengeId.toString(),
                        "123456",
                        CLIENT
                )
        );

        assertEquals("PASSWORD_RECOVERY_INVALID", exception.getErrorCode());
        verify(challenge).registerFailedAttempt();
        verify(challengeRepository).save(challenge);
    }

    @Test
    void shouldIssueHashedSingleUseResetTokenAfterCorrectCode() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge = activeChallenge(user);
        UUID challengeId = UUID.randomUUID();
        prepareChallengeLookup(challengeId, user, challenge);
        when(secretHasher.codeMatches(7L, "123456", "code-hash"))
                .thenReturn(true);
        when(secretGenerator.generateResetToken()).thenReturn("raw-token");
        when(secretHasher.hashResetToken("raw-token"))
                .thenReturn("token-hash");

        VerifyPasswordRecoveryResponse response = service.verify(
                challengeId.toString(),
                "123456",
                CLIENT
        );

        assertEquals("raw-token", response.resetToken());
        assertEquals(NOW.plusMinutes(10), response.expiresAt());
        verify(challenge).authorizeReset(
                "token-hash",
                NOW.plusMinutes(10),
                NOW
        );
    }

    @Test
    void shouldRejectCurrentPasswordWithTheSameGenericError() {
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge = resetAuthorizedChallenge(user);
        prepareResetLookup(user, challenge);
        when(passwordEncoder.matches("same-password", "current-hash"))
                .thenReturn(true);

        PasswordRecoveryException exception = assertThrows(
                PasswordRecoveryException.class,
                () -> service.reset(
                        "raw-token",
                        "same-password",
                        CLIENT
                )
        );

        assertEquals("PASSWORD_RECOVERY_INVALID", exception.getErrorCode());
        verify(passwordEncoder, never()).encode(any());
        verify(challengeRepository, never()).delete(any());
    }

    @Test
    void shouldPreservePasswordWhitespaceAndConsumeResetToken() {
        String newPassword = "  new-password  ";
        UserAccount user = user(true);
        PasswordRecoveryChallenge challenge = resetAuthorizedChallenge(user);
        prepareResetLookup(user, challenge);
        when(passwordEncoder.matches(newPassword, "current-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded-hash");

        service.reset(" raw-token ", newPassword, CLIENT);

        verify(passwordEncoder).matches(newPassword, "current-hash");
        verify(passwordEncoder).encode(newPassword);
        verify(user).changePassword("encoded-hash");
        verify(userAccountRepository).save(user);
        verify(challengeRepository).delete(challenge);
    }

    @Test
    void shouldRejectPasswordThatExceedsSeventyTwoUtf8Bytes() {
        String oversizedPassword = "😀".repeat(19);

        PasswordRecoveryException exception = assertThrows(
                PasswordRecoveryException.class,
                () -> service.reset(
                        "raw-token",
                        oversizedPassword,
                        CLIENT
                )
        );

        assertEquals("PASSWORD_RECOVERY_INVALID", exception.getErrorCode());
        verifyNoInteractions(secretHasher, challengeRepository);
    }

    private void prepareVerifiedAccount(UserAccount user) {
        when(userLockRepository.findByEmailForUpdate("user@devtasker.test"))
                .thenReturn(Optional.of(user));
    }

    private void prepareChallengeLookup(
            UUID challengeId,
            UserAccount user,
            PasswordRecoveryChallenge challenge
    ) {
        when(challengeRepository.findUserIdByPublicId(challengeId))
                .thenReturn(Optional.of(7L));
        when(userLockRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByPublicIdForUpdate(challengeId))
                .thenReturn(Optional.of(challenge));
        lenient().when(challenge.getUser()).thenReturn(user);
    }

    private void prepareResetLookup(
            UserAccount user,
            PasswordRecoveryChallenge challenge
    ) {
        when(secretHasher.hashResetToken("raw-token"))
                .thenReturn("token-hash");
        when(challengeRepository.findUserIdByResetTokenHash("token-hash"))
                .thenReturn(Optional.of(7L));
        when(userLockRepository.findByIdForUpdate(7L))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByResetTokenHashForUpdate("token-hash"))
                .thenReturn(Optional.of(challenge));
    }

    private static UserAccount user(boolean verified) {
        UserAccount user = org.mockito.Mockito.mock(UserAccount.class);
        lenient().when(user.getId()).thenReturn(7L);
        lenient().when(user.getName()).thenReturn("Dev User");
        lenient().when(user.getEmail()).thenReturn("user@devtasker.test");
        lenient().when(user.getPasswordHash()).thenReturn("current-hash");
        lenient().when(user.isEmailVerified()).thenReturn(verified);
        return user;
    }

    private static PasswordRecoveryChallenge activeChallenge(
            UserAccount user
    ) {
        PasswordRecoveryChallenge challenge =
                org.mockito.Mockito.mock(PasswordRecoveryChallenge.class);
        when(challenge.getUser()).thenReturn(user);
        when(challenge.getCodeHash()).thenReturn("code-hash");
        when(challenge.getVerifiedAt()).thenReturn(null);
        when(challenge.isCodeExpiredAt(NOW)).thenReturn(false);
        when(challenge.hasReachedAttemptLimit(5)).thenReturn(false);
        return challenge;
    }

    private static PasswordRecoveryChallenge resetAuthorizedChallenge(
            UserAccount user
    ) {
        PasswordRecoveryChallenge challenge =
                org.mockito.Mockito.mock(PasswordRecoveryChallenge.class);
        when(challenge.getUser()).thenReturn(user);
        when(challenge.hasActiveResetTokenAt(NOW)).thenReturn(true);
        return challenge;
    }
}

package br.com.devtasker.api.auth.passwordrecovery.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class PasswordRecoveryService {

    private static final int MAXIMUM_EMAIL_LENGTH = 255;
    private static final int MAXIMUM_TOKEN_LENGTH = 128;
    private static final int MAXIMUM_FINGERPRINT_SOURCE_LENGTH = 512;
    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_BCRYPT_BYTES = 72;

    private static final Duration RATE_LIMIT_WINDOW =
            Duration.ofMinutes(10);

    private static final int REQUESTS_PER_CLIENT = 20;
    private static final int REQUESTS_PER_IDENTIFIER = 5;
    private static final int RESENDS_PER_CLIENT = 30;
    private static final int RESENDS_PER_CHALLENGE = 5;
    private static final int VERIFICATIONS_PER_CLIENT = 60;
    private static final int VERIFICATIONS_PER_CHALLENGE = 10;
    private static final int RESETS_PER_CLIENT = 30;
    private static final int RESETS_PER_TOKEN = 5;

    private final UserAccountRepository userAccountRepository;
    private final PasswordRecoveryUserLockRepository userLockRepository;
    private final PasswordRecoveryChallengeRepository challengeRepository;
    private final PasswordRecoverySecretGenerator secretGenerator;
    private final PasswordRecoverySecretHasher secretHasher;
    private final PasswordRecoveryEmailDispatcher emailDispatcher;
    private final PasswordRecoveryRateLimiter rateLimiter;
    private final PasswordRecoveryFingerprintService fingerprintService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRecoveryProperties properties;
    private final Clock clock;

    public PasswordRecoveryService(
            UserAccountRepository userAccountRepository,
            PasswordRecoveryUserLockRepository userLockRepository,
            PasswordRecoveryChallengeRepository challengeRepository,
            PasswordRecoverySecretGenerator secretGenerator,
            PasswordRecoverySecretHasher secretHasher,
            PasswordRecoveryEmailDispatcher emailDispatcher,
            PasswordRecoveryRateLimiter rateLimiter,
            PasswordRecoveryFingerprintService fingerprintService,
            PasswordEncoder passwordEncoder,
            PasswordRecoveryProperties properties,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.userLockRepository = userLockRepository;
        this.challengeRepository = challengeRepository;
        this.secretGenerator = secretGenerator;
        this.secretHasher = secretHasher;
        this.emailDispatcher = emailDispatcher;
        this.rateLimiter = rateLimiter;
        this.fingerprintService = fingerprintService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public PasswordRecoveryChallengeResponse request(
            String email,
            String clientFingerprint
    ) {
        OffsetDateTime now = now();
        UUID responseId = UUID.randomUUID();
        PasswordRecoveryChallengeResponse neutralResponse =
                neutralChallengeResponse(responseId, now);

        String normalizedEmail = normalizeEmailOrNull(email);
        String emailFingerprint = identifierFingerprint(
                "request-email",
                normalizedEmail == null ? email : normalizedEmail
        );

        if (
                !allow(
                        "request-client",
                        clientFingerprint,
                        REQUESTS_PER_CLIENT
                )
                || !allow(
                        "request-identifier",
                        emailFingerprint,
                        REQUESTS_PER_IDENTIFIER
                )
                || normalizedEmail == null
        ) {
            return neutralResponse;
        }

        UserAccount user = userLockRepository
                .findByEmailForUpdate(normalizedEmail)
                .orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return neutralResponse;
        }

        PasswordRecoveryChallenge challenge = challengeRepository
                .findByUserIdForUpdate(user.getId())
                .orElse(null);

        if (
                challenge != null
                && (
                        challenge.hasActiveResetTokenAt(now)
                        || !challenge.canBeResentAt(
                                now,
                                properties.resendInterval()
                        )
                )
        ) {
            return neutralResponse;
        }

        String rawCode = secretGenerator.generateCode();
        String codeHash = secretHasher.hashCode(
                user.getId(),
                rawCode
        );
        OffsetDateTime expiresAt = now.plus(properties.codeExpiration());

        if (challenge == null) {
            challenge = PasswordRecoveryChallenge.create(
                    user,
                    responseId,
                    codeHash,
                    expiresAt,
                    now
            );
        } else {
            challenge.restart(
                    responseId,
                    codeHash,
                    expiresAt,
                    now
            );
        }

        challengeRepository.saveAndFlush(challenge);
        scheduleEmailAfterCommit(user, rawCode);

        return neutralResponse;
    }

    @Transactional
    public void resend(
            String rawChallengeId,
            String clientFingerprint
    ) {
        String challengeFingerprint = identifierFingerprint(
                "resend-challenge",
                rawChallengeId
        );

        if (
                !allow(
                        "resend-client",
                        clientFingerprint,
                        RESENDS_PER_CLIENT
                )
                || !allow(
                        "resend-challenge",
                        challengeFingerprint,
                        RESENDS_PER_CHALLENGE
                )
        ) {
            return;
        }

        UUID challengeId = parseUuidOrNull(rawChallengeId);

        if (challengeId == null) {
            return;
        }

        Long userId = challengeRepository
                .findUserIdByPublicId(challengeId)
                .orElse(null);
        UserAccount user = userLockRepository
                .findByIdForUpdate(userId)
                .orElse(null);

        if (user == null || !user.isEmailVerified()) {
            return;
        }

        PasswordRecoveryChallenge challenge = challengeRepository
                .findByPublicIdForUpdate(challengeId)
                .filter(value -> sameUser(value, user))
                .orElse(null);

        if (challenge == null) {
            return;
        }

        OffsetDateTime now = now();

        if (
                challenge.hasActiveResetTokenAt(now)
                || !challenge.canBeResentAt(
                        now,
                        properties.resendInterval()
                )
        ) {
            return;
        }

        String rawCode = secretGenerator.generateCode();
        String codeHash = secretHasher.hashCode(
                user.getId(),
                rawCode
        );

        challenge.renewCode(
                codeHash,
                now.plus(properties.codeExpiration()),
                now
        );
        challengeRepository.saveAndFlush(challenge);
        scheduleEmailAfterCommit(user, rawCode);
    }

    @Transactional(noRollbackFor = PasswordRecoveryException.class)
    public VerifyPasswordRecoveryResponse verify(
            String rawChallengeId,
            String rawCode,
            String clientFingerprint
    ) {
        String challengeFingerprint = identifierFingerprint(
                "verify-challenge",
                rawChallengeId
        );

        enforceLimit(
                "verify-client",
                clientFingerprint,
                VERIFICATIONS_PER_CLIENT
        );
        enforceLimit(
                "verify-challenge",
                challengeFingerprint,
                VERIFICATIONS_PER_CHALLENGE
        );

        UUID challengeId = parseUuidOrInvalid(rawChallengeId);
        String normalizedCode = normalizeCodeOrInvalid(rawCode);
        Long userId = challengeRepository
                .findUserIdByPublicId(challengeId)
                .orElseThrow(PasswordRecoveryException::invalid);
        UserAccount user = userLockRepository
                .findByIdForUpdate(userId)
                .orElseThrow(PasswordRecoveryException::invalid);
        PasswordRecoveryChallenge challenge = challengeRepository
                .findByPublicIdForUpdate(challengeId)
                .filter(value -> sameUser(value, user))
                .orElseThrow(PasswordRecoveryException::invalid);

        OffsetDateTime now = now();

        if (
                !user.isEmailVerified()
                || challenge.getVerifiedAt() != null
                || challenge.isCodeExpiredAt(now)
                || challenge.hasReachedAttemptLimit(
                        properties.maximumAttempts()
                )
        ) {
            throw PasswordRecoveryException.invalid();
        }

        boolean codeMatches = secretHasher.codeMatches(
                user.getId(),
                normalizedCode,
                challenge.getCodeHash()
        );

        if (!codeMatches) {
            challenge.registerFailedAttempt();
            challengeRepository.save(challenge);
            throw PasswordRecoveryException.invalid();
        }

        String resetToken = secretGenerator.generateResetToken();
        String resetTokenHash = secretHasher.hashResetToken(resetToken);
        OffsetDateTime resetTokenExpiresAt =
                now.plus(properties.resetTokenExpiration());

        challenge.authorizeReset(
                resetTokenHash,
                resetTokenExpiresAt,
                now
        );
        challengeRepository.save(challenge);

        return new VerifyPasswordRecoveryResponse(
                resetToken,
                resetTokenExpiresAt
        );
    }

    @Transactional
    public void reset(
            String rawResetToken,
            String newPassword,
            String clientFingerprint
    ) {
        String tokenFingerprint = identifierFingerprint(
                "reset-token",
                rawResetToken
        );

        enforceLimit(
                "reset-client",
                clientFingerprint,
                RESETS_PER_CLIENT
        );
        enforceLimit(
                "reset-token",
                tokenFingerprint,
                RESETS_PER_TOKEN
        );

        String resetToken = normalizeResetTokenOrInvalid(rawResetToken);
        validateNewPassword(newPassword);

        String resetTokenHash = secretHasher.hashResetToken(resetToken);
        Long userId = challengeRepository
                .findUserIdByResetTokenHash(resetTokenHash)
                .orElseThrow(PasswordRecoveryException::invalid);
        UserAccount user = userLockRepository
                .findByIdForUpdate(userId)
                .orElseThrow(PasswordRecoveryException::invalid);
        PasswordRecoveryChallenge challenge = challengeRepository
                .findByResetTokenHashForUpdate(resetTokenHash)
                .filter(value -> sameUser(value, user))
                .orElseThrow(PasswordRecoveryException::invalid);

        OffsetDateTime now = now();

        if (!challenge.hasActiveResetTokenAt(now)) {
            throw PasswordRecoveryException.invalid();
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw PasswordRecoveryException.invalid();
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);
        userAccountRepository.save(user);
        challengeRepository.delete(challenge);
    }

    private PasswordRecoveryChallengeResponse neutralChallengeResponse(
            UUID challengeId,
            OffsetDateTime now
    ) {
        return new PasswordRecoveryChallengeResponse(
                challengeId,
                now.plus(properties.codeExpiration()),
                now.plus(properties.resendInterval())
        );
    }

    private void scheduleEmailAfterCommit(
            UserAccount user,
            String rawCode
    ) {
        try {
            emailDispatcher.dispatchAfterCommit(
                    new PasswordRecoveryEmailMessage(
                            user.getName(),
                            user.getEmail(),
                            rawCode,
                            properties.expirationMinutes()
                    )
            );
        } catch (RuntimeException ignored) {
            // Falhas de preparação não alteram a resposta pública.
        }
    }

    private boolean allow(
            String scope,
            String fingerprint,
            int maximumRequests
    ) {
        return rateLimiter.allow(
                scope,
                safeFingerprint(fingerprint),
                maximumRequests,
                RATE_LIMIT_WINDOW
        );
    }

    private void enforceLimit(
            String scope,
            String fingerprint,
            int maximumRequests
    ) {
        if (!allow(scope, fingerprint, maximumRequests)) {
            throw PasswordRecoveryException.invalid();
        }
    }

    private String identifierFingerprint(
            String namespace,
            String rawIdentifier
    ) {
        String bounded = rawIdentifier == null
                ? "unknown"
                : rawIdentifier.substring(
                        0,
                        Math.min(
                                rawIdentifier.length(),
                                MAXIMUM_FINGERPRINT_SOURCE_LENGTH
                        )
                );

        return fingerprintService.identifierFingerprint(
                namespace,
                bounded
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String normalizeEmailOrNull(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (
                normalized.isEmpty()
                || normalized.length() > MAXIMUM_EMAIL_LENGTH
                || normalized.chars().anyMatch(Character::isWhitespace)
        ) {
            return null;
        }

        int atIndex = normalized.indexOf('@');

        if (
                atIndex <= 0
                || atIndex != normalized.lastIndexOf('@')
                || atIndex == normalized.length() - 1
        ) {
            return null;
        }

        return normalized;
    }

    private String normalizeCodeOrInvalid(String rawCode) {
        if (rawCode == null) {
            throw PasswordRecoveryException.invalid();
        }

        String code = rawCode.trim();

        if (code.length() != properties.codeLength()) {
            throw PasswordRecoveryException.invalid();
        }

        for (int index = 0; index < code.length(); index++) {
            char character = code.charAt(index);

            if (character < '0' || character > '9') {
                throw PasswordRecoveryException.invalid();
            }
        }

        return code;
    }

    private static UUID parseUuidOrInvalid(String rawValue) {
        UUID value = parseUuidOrNull(rawValue);

        if (value == null) {
            throw PasswordRecoveryException.invalid();
        }

        return value;
    }

    private static UUID parseUuidOrNull(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim();

        try {
            UUID value = UUID.fromString(normalized);
            return value.toString().equalsIgnoreCase(normalized)
                    ? value
                    : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String normalizeResetTokenOrInvalid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw PasswordRecoveryException.invalid();
        }

        String token = rawToken.trim();

        if (token.length() > MAXIMUM_TOKEN_LENGTH) {
            throw PasswordRecoveryException.invalid();
        }

        return token;
    }

    private static void validateNewPassword(String password) {
        if (
                password == null
                || password.isBlank()
                || password.length() < MINIMUM_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length
                        > MAXIMUM_BCRYPT_BYTES
        ) {
            throw PasswordRecoveryException.invalid();
        }
    }

    private static boolean sameUser(
            PasswordRecoveryChallenge challenge,
            UserAccount user
    ) {
        return challenge.getUser().getId().equals(user.getId());
    }

    private static String safeFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return "unknown";
        }

        return fingerprint;
    }
}

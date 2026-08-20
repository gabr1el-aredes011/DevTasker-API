package br.com.devtasker.api.auth.passwordrecovery.domain;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "password_recovery_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordRecoveryChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_sent_at", nullable = false)
    private OffsetDateTime lastSentAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "reset_token_hash", length = 255, unique = true)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private OffsetDateTime resetTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private PasswordRecoveryChallenge(
            UserAccount user,
            UUID publicId,
            String codeHash,
            OffsetDateTime expiresAt,
            OffsetDateTime lastSentAt
    ) {
        validateUser(user);
        validatePublicId(publicId);
        validateHash(codeHash, "O hash do código é obrigatório.");
        validateMoment(expiresAt, "A expiração do código é obrigatória.");
        validateMoment(lastSentAt, "O instante de envio é obrigatório.");

        this.user = user;
        this.publicId = publicId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
        this.attemptCount = 0;
    }

    public static PasswordRecoveryChallenge create(
            UserAccount user,
            UUID publicId,
            String codeHash,
            OffsetDateTime expiresAt,
            OffsetDateTime lastSentAt
    ) {
        return new PasswordRecoveryChallenge(
                user,
                publicId,
                codeHash,
                expiresAt,
                lastSentAt
        );
    }

    public void restart(
            UUID newPublicId,
            String newCodeHash,
            OffsetDateTime newExpiresAt,
            OffsetDateTime sentAt
    ) {
        validatePublicId(newPublicId);
        validateHash(newCodeHash, "O hash do código é obrigatório.");
        validateMoment(newExpiresAt, "A expiração do código é obrigatória.");
        validateMoment(sentAt, "O instante de envio é obrigatório.");

        this.publicId = newPublicId;
        this.codeHash = newCodeHash;
        this.expiresAt = newExpiresAt;
        this.lastSentAt = sentAt;
        this.attemptCount = 0;
        invalidateResetAuthorization();
    }

    public void renewCode(
            String newCodeHash,
            OffsetDateTime newExpiresAt,
            OffsetDateTime sentAt
    ) {
        restart(
                publicId,
                newCodeHash,
                newExpiresAt,
                sentAt
        );
    }

    public void registerFailedAttempt() {
        attemptCount++;
    }

    public boolean hasReachedAttemptLimit(int maximumAttempts) {
        if (maximumAttempts <= 0) {
            throw new IllegalArgumentException(
                    "O limite de tentativas deve ser positivo."
            );
        }

        return attemptCount >= maximumAttempts;
    }

    public boolean isCodeExpiredAt(OffsetDateTime instant) {
        validateMoment(instant, "O instante de comparação é obrigatório.");
        return !instant.isBefore(expiresAt);
    }

    public boolean canBeResentAt(
            OffsetDateTime instant,
            Duration resendInterval
    ) {
        validateMoment(instant, "O instante de comparação é obrigatório.");

        if (resendInterval == null || resendInterval.isNegative()) {
            throw new IllegalArgumentException(
                    "O intervalo de reenvio não pode ser negativo."
            );
        }

        return !instant.isBefore(lastSentAt.plus(resendInterval));
    }

    public void authorizeReset(
            String tokenHash,
            OffsetDateTime tokenExpiresAt,
            OffsetDateTime instant
    ) {
        validateHash(tokenHash, "O hash do token é obrigatório.");
        validateMoment(tokenExpiresAt, "A expiração do token é obrigatória.");
        validateMoment(instant, "O instante da verificação é obrigatório.");

        if (!tokenExpiresAt.isAfter(instant)) {
            throw new IllegalArgumentException(
                    "A expiração do token deve estar no futuro."
            );
        }

        this.verifiedAt = instant;
        this.resetTokenHash = tokenHash;
        this.resetTokenExpiresAt = tokenExpiresAt;
    }

    public boolean hasActiveResetTokenAt(OffsetDateTime instant) {
        validateMoment(instant, "O instante de comparação é obrigatório.");

        return verifiedAt != null
                && resetTokenHash != null
                && resetTokenExpiresAt != null
                && instant.isBefore(resetTokenExpiresAt);
    }

    private void invalidateResetAuthorization() {
        this.verifiedAt = null;
        this.resetTokenHash = null;
        this.resetTokenExpiresAt = null;
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static void validateUser(UserAccount user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "O usuário persistido é obrigatório."
            );
        }
    }

    private static void validatePublicId(UUID publicId) {
        if (publicId == null) {
            throw new IllegalArgumentException(
                    "O identificador público é obrigatório."
            );
        }
    }

    private static void validateHash(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateMoment(
            OffsetDateTime value,
            String message
    ) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}

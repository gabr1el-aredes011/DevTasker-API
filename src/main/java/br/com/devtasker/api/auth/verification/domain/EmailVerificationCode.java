package br.com.devtasker.api.auth.verification.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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
@Table(name = "email_verification_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name =
                            "fk_email_verification_codes_user"
            )
    )
    private UserAccount user;

    @Column(
            name = "code_hash",
            nullable = false,
            length = 255
    )
    private String codeHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private OffsetDateTime expiresAt;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private int attemptCount;

    @Column(
            name = "last_sent_at",
            nullable = false
    )
    private OffsetDateTime lastSentAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    private EmailVerificationCode(
            UserAccount user,
            String codeHash,
            OffsetDateTime expiresAt
    ) {
        validateUser(user);
        validateCodeHash(codeHash);
        validateExpiration(expiresAt);

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attemptCount = 0;
        this.lastSentAt = now;
    }

    public static EmailVerificationCode create(
            UserAccount user,
            String codeHash,
            OffsetDateTime expiresAt
    ) {
        return new EmailVerificationCode(
                user,
                codeHash,
                expiresAt
        );
    }

    public void renew(
            String newCodeHash,
            OffsetDateTime newExpiresAt
    ) {
        validateCodeHash(newCodeHash);
        validateExpiration(newExpiresAt);

        this.codeHash = newCodeHash;
        this.expiresAt = newExpiresAt;
        this.attemptCount = 0;
        this.lastSentAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void registerFailedAttempt() {
        this.attemptCount++;
    }

    public boolean hasReachedAttemptLimit(
            int maximumAttempts
    ) {
        if (maximumAttempts <= 0) {
            throw new IllegalArgumentException(
                    "O limite de tentativas deve ser positivo."
            );
        }

        return attemptCount >= maximumAttempts;
    }

    public boolean isExpiredAt(
            OffsetDateTime referenceTime
    ) {
        if (referenceTime == null) {
            throw new IllegalArgumentException(
                    "O horário de referência é obrigatório."
            );
        }

        return !referenceTime.isBefore(expiresAt);
    }

    public boolean canBeResentAt(
            OffsetDateTime referenceTime,
            long minimumIntervalSeconds
    ) {
        if (referenceTime == null) {
            throw new IllegalArgumentException(
                    "O horário de referência é obrigatório."
            );
        }

        if (minimumIntervalSeconds < 0) {
            throw new IllegalArgumentException(
                    "O intervalo não pode ser negativo."
            );
        }

        return !referenceTime.isBefore(
                lastSentAt.plusSeconds(
                        minimumIntervalSeconds
                )
        );
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        this.updatedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static void validateUser(
            UserAccount user
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "O usuário é obrigatório."
            );
        }
    }

    private static void validateCodeHash(
            String codeHash
    ) {
        if (
                codeHash == null ||
                codeHash.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O hash do código é obrigatório."
            );
        }
    }

    private static void validateExpiration(
            OffsetDateTime expiresAt
    ) {
        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "A expiração é obrigatória."
            );
        }
    }
}
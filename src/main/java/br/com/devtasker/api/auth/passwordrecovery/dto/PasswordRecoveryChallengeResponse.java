package br.com.devtasker.api.auth.passwordrecovery.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasswordRecoveryChallengeResponse(
        UUID challengeId,
        OffsetDateTime expiresAt,
        OffsetDateTime resendAvailableAt
) {
}

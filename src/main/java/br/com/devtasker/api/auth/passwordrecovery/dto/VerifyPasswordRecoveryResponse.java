package br.com.devtasker.api.auth.passwordrecovery.dto;

import java.time.OffsetDateTime;

public record VerifyPasswordRecoveryResponse(
        String resetToken,
        OffsetDateTime expiresAt
) {
}

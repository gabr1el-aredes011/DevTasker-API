package br.com.devtasker.api.auth.verification.service;

import java.time.OffsetDateTime;

public record IssuedEmailVerificationCode(
        String rawCode,
        OffsetDateTime expiresAt
) {
}
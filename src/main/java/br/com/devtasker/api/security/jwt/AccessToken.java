package br.com.devtasker.api.security.jwt;

import java.time.Instant;

public record AccessToken(
        String value,
        Instant expiresAt
) {
}
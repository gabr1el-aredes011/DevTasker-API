package br.com.devtasker.api.auth.dto;

import java.time.Instant;

import br.com.devtasker.api.user.domain.UserRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        Long userId,
        String name,
        String email,
        UserRole role
) {
}

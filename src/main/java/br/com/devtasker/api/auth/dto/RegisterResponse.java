package br.com.devtasker.api.auth.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.user.domain.UserRole;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        OffsetDateTime createdAt
) {
}
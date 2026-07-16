package br.com.devtasker.api.user.dto;

import br.com.devtasker.api.user.domain.UserRole;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
}
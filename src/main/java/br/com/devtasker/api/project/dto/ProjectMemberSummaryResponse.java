package br.com.devtasker.api.project.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectMemberRole;

public record ProjectMemberSummaryResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String profileImageUrl,
        ProjectMemberRole role,
        OffsetDateTime joinedAt,
        boolean currentUser
) {
}

package br.com.devtasker.api.project.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectMemberRole;

public record ProjectSummaryResponse(
        Long id,
        String name,
        String description,
        ProjectMemberRole membershipRole,
        OffsetDateTime createdAt
) {
}

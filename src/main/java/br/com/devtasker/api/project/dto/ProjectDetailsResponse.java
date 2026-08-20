package br.com.devtasker.api.project.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectMemberRole;

public record ProjectDetailsResponse(
        Long id,
        String name,
        String description,
        ProjectMemberRole membershipRole,
        Long ownerId,
        String ownerName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
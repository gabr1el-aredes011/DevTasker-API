package br.com.devtasker.api.dashboard.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectMemberRole;

public record DashboardRecentProjectResponse(
        Long id,
        String name,
        String description,
        ProjectMemberRole membershipRole,
        OffsetDateTime createdAt
) {
}
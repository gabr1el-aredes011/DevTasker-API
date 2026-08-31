package br.com.devtasker.api.project.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectMemberRole;

public record ProjectInvitationSummaryResponse(
        Long id,
        String invitedEmail,
        ProjectMemberRole role,
        String invitedByName,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
}

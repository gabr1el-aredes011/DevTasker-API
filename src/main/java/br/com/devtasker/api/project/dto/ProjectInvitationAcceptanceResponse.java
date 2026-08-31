package br.com.devtasker.api.project.dto;

public record ProjectInvitationAcceptanceResponse(
        Long projectId,
        String projectName,
        ProjectMemberSummaryResponse membership
) {
}

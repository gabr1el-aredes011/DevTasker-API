package br.com.devtasker.api.project.dto;

import br.com.devtasker.api.project.domain.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberRoleRequest(
        @NotNull(message = "Selecione a nova função do membro.")
        ProjectMemberRole role
) {
}

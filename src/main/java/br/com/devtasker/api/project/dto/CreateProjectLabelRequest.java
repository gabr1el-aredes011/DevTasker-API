package br.com.devtasker.api.project.dto;

import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.domain.ProjectLabelColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectLabelRequest(
        @NotBlank(message = "O nome da label é obrigatório.")
        @Size(
                max = ProjectLabel.MAXIMUM_NAME_LENGTH,
                message = "O nome da label deve possuir no máximo 30 caracteres."
        )
        String name,

        @NotNull(message = "A cor da label é obrigatória.")
        ProjectLabelColor color
) {
}

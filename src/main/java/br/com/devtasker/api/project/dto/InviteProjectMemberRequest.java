package br.com.devtasker.api.project.dto;

import br.com.devtasker.api.project.domain.ProjectMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteProjectMemberRequest(
        @NotBlank(message = "Informe o e-mail da pessoa convidada.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 255, message = "O e-mail deve possuir no máximo 255 caracteres.")
        String email,

        @NotNull(message = "Selecione a função do novo membro.")
        ProjectMemberRole role
) {
}

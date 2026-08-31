package br.com.devtasker.api.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptProjectInvitationRequest(
        @NotBlank(message = "O token do convite é obrigatório.")
        @Size(max = 200, message = "O token do convite possui formato inválido.")
        String token
) {
}

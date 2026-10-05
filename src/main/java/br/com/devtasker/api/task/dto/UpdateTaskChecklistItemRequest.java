package br.com.devtasker.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskChecklistItemRequest(
        @NotBlank(message = "O título do item é obrigatório.")
        @Size(max = 180, message = "O título do item deve possuir no máximo 180 caracteres.")
        String title,

        @NotNull(message = "O status do item é obrigatório.")
        Boolean completed
) {
}

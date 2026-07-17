package br.com.devtasker.api.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MoveTaskRequest(

        @NotNull(message = "A coluna de destino é obrigatória.")
        Long targetColumnId,

        @NotNull(message = "A posição de destino é obrigatória.")
        @PositiveOrZero(
                message = "A posição de destino não pode ser negativa."
        )
        Integer targetPosition

) {
}
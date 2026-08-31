package br.com.devtasker.api.task.dto;

import java.time.LocalDate;

import br.com.devtasker.api.task.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(

        @NotBlank(message = "O título é obrigatório.")
        @Size(
                max = 180,
                message = "O título deve possuir no máximo 180 caracteres."
        )
        String title,

        @Size(
                max = 4000,
                message = "A descrição deve possuir no máximo 4000 caracteres."
        )
        String description,

        @NotNull(message = "A prioridade é obrigatória.")
        TaskPriority priority,

        LocalDate dueDate,

        Long assigneeId

) {
}

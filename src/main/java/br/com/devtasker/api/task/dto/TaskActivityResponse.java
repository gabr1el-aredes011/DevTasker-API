package br.com.devtasker.api.task.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.task.domain.TaskActivityType;

public record TaskActivityResponse(
        Long id,
        TaskActivityType type,
        String description,
        TaskUserSummaryResponse actor,
        OffsetDateTime createdAt
) {
}

package br.com.devtasker.api.task.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.devtasker.api.task.domain.TaskPriority;

public record TaskResponse(
        Long id,
        Long columnId,
        String title,
        String description,
        TaskPriority priority,
        LocalDate dueDate,
        Integer position,
        TaskUserSummaryResponse creator,
        TaskUserSummaryResponse assignee,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
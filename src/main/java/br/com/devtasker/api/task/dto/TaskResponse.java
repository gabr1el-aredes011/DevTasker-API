package br.com.devtasker.api.task.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

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
        List<String> labels,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

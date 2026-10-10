package br.com.devtasker.api.task.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.task.domain.TaskTechnology;

public record TaskResponse(
        Long id,
        Long columnId,
        String title,
        String description,
        TaskPriority priority,
        LocalDate dueDate,
        Integer position,
        TaskUserSummaryResponse creator,
        List<TaskUserSummaryResponse> assignees,
        List<TaskLabelResponse> labels,
        List<TaskTechnology> technologies,
        List<TaskChecklistItemResponse> checklistItems,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

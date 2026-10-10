package br.com.devtasker.api.dashboard.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.task.domain.TaskActivityType;

public record DashboardActivityResponse(
        Long id,
        TaskActivityType type,
        String description,
        OffsetDateTime createdAt,
        Long actorId,
        String actorName,
        String actorProfileImageUrl,
        Long taskId,
        String taskTitle,
        Long boardId,
        Long projectId,
        String projectName
) {
}

package br.com.devtasker.api.dashboard.dto;

import java.time.LocalDate;

import br.com.devtasker.api.task.domain.TaskPriority;

public record DashboardAttentionTaskResponse(
        Long id,
        String title,
        TaskPriority priority,
        LocalDate dueDate,
        String columnName,
        Long boardId,
        String boardName,
        Long projectId,
        String projectName,
        boolean overdue
) {
}
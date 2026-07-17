package br.com.devtasker.api.board.dto;

import java.time.LocalDate;

import br.com.devtasker.api.task.domain.TaskPriority;

public record KanbanTaskResponse(
        Long id,
        String title,
        TaskPriority priority,
        LocalDate dueDate,
        Integer position,
        Long assigneeId,
        String assigneeName
) {
}
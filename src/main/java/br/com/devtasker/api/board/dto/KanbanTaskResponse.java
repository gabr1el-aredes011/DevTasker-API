package br.com.devtasker.api.board.dto;

import java.time.LocalDate;
import java.util.List;

import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.task.domain.TaskTechnology;
import br.com.devtasker.api.task.dto.TaskLabelResponse;
import br.com.devtasker.api.task.dto.TaskUserSummaryResponse;

public record KanbanTaskResponse(
        Long id,
        String title,
        TaskPriority priority,
        LocalDate dueDate,
        Integer position,
        List<TaskUserSummaryResponse> assignees,
        List<TaskLabelResponse> labels,
        List<TaskTechnology> technologies,
        int completedChecklistItems,
        int totalChecklistItems
) {
}

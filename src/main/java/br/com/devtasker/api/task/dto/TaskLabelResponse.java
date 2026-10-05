package br.com.devtasker.api.task.dto;

import br.com.devtasker.api.project.domain.ProjectLabelColor;

public record TaskLabelResponse(
        Long id,
        String name,
        ProjectLabelColor color,
        boolean archived
) {
}

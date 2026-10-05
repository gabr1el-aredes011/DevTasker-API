package br.com.devtasker.api.project.dto;

import java.time.OffsetDateTime;

import br.com.devtasker.api.project.domain.ProjectLabelColor;

public record ProjectLabelResponse(
        Long id,
        String name,
        ProjectLabelColor color,
        long usageCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

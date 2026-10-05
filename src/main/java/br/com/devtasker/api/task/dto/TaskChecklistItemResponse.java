package br.com.devtasker.api.task.dto;

import java.time.OffsetDateTime;

public record TaskChecklistItemResponse(
        Long id,
        String title,
        boolean completed,
        Integer position,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

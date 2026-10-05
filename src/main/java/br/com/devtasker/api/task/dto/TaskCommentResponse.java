package br.com.devtasker.api.task.dto;

import java.time.OffsetDateTime;

public record TaskCommentResponse(
        Long id,
        String content,
        TaskUserSummaryResponse author,
        boolean canEdit,
        boolean canDelete,
        boolean edited,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

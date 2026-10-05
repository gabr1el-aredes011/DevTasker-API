package br.com.devtasker.api.task.dto;

import java.time.OffsetDateTime;

public record TaskAttachmentResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        TaskUserSummaryResponse uploader,
        boolean canDelete,
        OffsetDateTime createdAt
) {
}

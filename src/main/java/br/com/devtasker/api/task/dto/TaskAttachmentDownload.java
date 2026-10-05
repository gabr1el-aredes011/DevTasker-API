package br.com.devtasker.api.task.dto;

import org.springframework.core.io.Resource;

public record TaskAttachmentDownload(
        Resource resource,
        String originalFileName,
        String contentType,
        long sizeBytes
) {
}

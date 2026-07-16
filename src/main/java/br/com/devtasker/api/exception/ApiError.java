package br.com.devtasker.api.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        Map<String, String> fields
) {
}

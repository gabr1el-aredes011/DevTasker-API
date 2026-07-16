package br.com.devtasker.api.task.dto;

public record TaskUserSummaryResponse(
        Long id,
        String name,
        String profileImageUrl
) {
}
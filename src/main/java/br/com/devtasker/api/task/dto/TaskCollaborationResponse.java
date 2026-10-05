package br.com.devtasker.api.task.dto;

import java.util.List;

public record TaskCollaborationResponse(
        List<TaskCommentResponse> comments,
        List<TaskActivityResponse> activities
) {
}

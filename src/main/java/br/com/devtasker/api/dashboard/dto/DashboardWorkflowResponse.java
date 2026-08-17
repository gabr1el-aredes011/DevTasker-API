package br.com.devtasker.api.dashboard.dto;

public record DashboardWorkflowResponse(
        long backlog,
        long todo,
        long doing,
        long review,
        long done
) {
}
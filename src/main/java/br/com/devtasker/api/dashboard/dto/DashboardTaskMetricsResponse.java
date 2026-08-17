package br.com.devtasker.api.dashboard.dto;

public record DashboardTaskMetricsResponse(
        long total,
        long active,
        long doing,
        long completed,
        long overdue
) {
}
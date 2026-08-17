package br.com.devtasker.api.dashboard.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long projectCount,
        long boardCount,
        DashboardTaskMetricsResponse taskMetrics,
        List<DashboardRecentProjectResponse> recentProjects,
        List<DashboardAttentionTaskResponse> attentionTasks,
        DashboardWorkflowResponse workflow
) {
}
package br.com.devtasker.api.dashboard.service;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.dashboard.dto.DashboardSummaryResponse;
import br.com.devtasker.api.dashboard.repository.DashboardQueryRepository;

@Service
public class DashboardService {

    private final DashboardQueryRepository
            dashboardQueryRepository;

    public DashboardService(
            DashboardQueryRepository dashboardQueryRepository
    ) {
        this.dashboardQueryRepository =
                dashboardQueryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(
            Long userId
    ) {
        LocalDate today =
                LocalDate.now(ZoneOffset.UTC);

        return new DashboardSummaryResponse(
                dashboardQueryRepository
                        .countProjectsByUser(userId),

                dashboardQueryRepository
                        .countBoardsByUser(userId),

                dashboardQueryRepository
                        .findTaskMetrics(
                                userId,
                                today
                        ),

                dashboardQueryRepository
                        .findRecentProjects(userId),

                dashboardQueryRepository
                        .findAttentionTasks(
                                userId,
                                today
                        ),

                dashboardQueryRepository
                        .findWorkflow(userId)
        );
    }
}
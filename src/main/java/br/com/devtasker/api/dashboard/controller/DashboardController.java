package br.com.devtasker.api.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.dashboard.dto.DashboardSummaryResponse;
import br.com.devtasker.api.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService
    ) {
        this.dashboardService =
                dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return dashboardService.getSummary(
                extractUserId(jwt)
        );
    }

    private Long extractUserId(
            Jwt jwt
    ) {
        Number userId =
                jwt.getClaim("user_id");

        return userId.longValue();
    }
}
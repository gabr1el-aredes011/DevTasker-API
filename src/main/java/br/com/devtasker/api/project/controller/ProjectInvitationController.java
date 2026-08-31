package br.com.devtasker.api.project.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.project.dto.AcceptProjectInvitationRequest;
import br.com.devtasker.api.project.dto.ProjectInvitationAcceptanceResponse;
import br.com.devtasker.api.project.service.ProjectInvitationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/project-invitations")
public class ProjectInvitationController {

    private final ProjectInvitationService invitationService;

    public ProjectInvitationController(ProjectInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/accept")
    public ProjectInvitationAcceptanceResponse accept(
            @Valid @RequestBody AcceptProjectInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = ((Number) jwt.getClaim("user_id")).longValue();
        return invitationService.accept(userId, request);
    }
}

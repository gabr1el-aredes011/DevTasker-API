package br.com.devtasker.api.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.project.dto.InviteProjectMemberRequest;
import br.com.devtasker.api.project.dto.ProjectInvitationSummaryResponse;
import br.com.devtasker.api.project.dto.ProjectMemberSummaryResponse;
import br.com.devtasker.api.project.dto.UpdateProjectMemberRoleRequest;
import br.com.devtasker.api.project.service.ProjectInvitationService;
import br.com.devtasker.api.project.service.ProjectMemberManagementService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectMemberManagementController {

    private final ProjectInvitationService invitationService;
    private final ProjectMemberManagementService memberManagementService;

    public ProjectMemberManagementController(
            ProjectInvitationService invitationService,
            ProjectMemberManagementService memberManagementService
    ) {
        this.invitationService = invitationService;
        this.memberManagementService = memberManagementService;
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectInvitationSummaryResponse invite(
            @PathVariable Long projectId,
            @Valid @RequestBody InviteProjectMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return invitationService.invite(projectId, userId(jwt), request);
    }

    @GetMapping("/invitations")
    public List<ProjectInvitationSummaryResponse> findPending(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return invitationService.findPending(projectId, userId(jwt));
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @PathVariable Long projectId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        invitationService.revoke(projectId, invitationId, userId(jwt));
    }

    @PutMapping("/members/{membershipId}/role")
    public ProjectMemberSummaryResponse changeRole(
            @PathVariable Long projectId,
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return memberManagementService.changeRole(projectId, membershipId, userId(jwt), request);
    }

    @DeleteMapping("/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable Long projectId,
            @PathVariable Long membershipId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        memberManagementService.remove(projectId, membershipId, userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return ((Number) jwt.getClaim("user_id")).longValue();
    }
}

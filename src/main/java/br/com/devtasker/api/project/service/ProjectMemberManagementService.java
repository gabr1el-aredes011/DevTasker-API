package br.com.devtasker.api.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectMembershipException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.ProjectMemberSummaryResponse;
import br.com.devtasker.api.project.dto.UpdateProjectMemberRoleRequest;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectMemberManagementService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService accessService;

    public ProjectMemberManagementService(
            ProjectMemberRepository memberRepository,
            ProjectAccessService accessService
    ) {
        this.memberRepository = memberRepository;
        this.accessService = accessService;
    }

    @Transactional
    public ProjectMemberSummaryResponse changeRole(
            Long projectId,
            Long membershipId,
            Long actorUserId,
            UpdateProjectMemberRoleRequest request
    ) {
        ProjectMember actor = accessService.requireManagementAccess(projectId, actorUserId);
        ProjectMember target = requireMember(projectId, membershipId);
        ProjectMemberRole requestedRole = request.role();

        requireManageableTarget(actor, target);
        requireAssignableRole(actor, requestedRole);

        target.changeRole(requestedRole);
        return toSummary(memberRepository.save(target), actorUserId);
    }

    @Transactional
    public void remove(Long projectId, Long membershipId, Long actorUserId) {
        ProjectMember actor = accessService.requireManagementAccess(projectId, actorUserId);
        ProjectMember target = requireMember(projectId, membershipId);

        requireManageableTarget(actor, target);
        memberRepository.delete(target);
    }

    public void requireAssignableRole(ProjectMember actor, ProjectMemberRole role) {
        if (role == ProjectMemberRole.OWNER) {
            throw invalidRole("A propriedade do projeto não pode ser concedida por esta operação.");
        }

        if (actor.getRole() == ProjectMemberRole.ADMIN && role == ProjectMemberRole.ADMIN) {
            throw denied("Administradores não podem conceder a função de administrador.");
        }
    }

    private ProjectMember requireMember(Long projectId, Long membershipId) {
        return memberRepository.findById(membershipId)
                .filter(member -> member.getProject().getId().equals(projectId))
                .filter(member -> member.getProject().getArchivedAt() == null)
                .orElseThrow(() -> new ProjectMembershipException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_MEMBER_NOT_FOUND",
                        "O membro solicitado não foi encontrado neste projeto."
                ));
    }

    private void requireManageableTarget(ProjectMember actor, ProjectMember target) {
        if (target.getRole() == ProjectMemberRole.OWNER) {
            throw invalidRole("O proprietário não pode ser removido nem ter sua função alterada.");
        }

        if (actor.getUser().getId().equals(target.getUser().getId())) {
            throw invalidRole("Você não pode alterar ou remover sua própria participação.");
        }

        if (actor.getRole() == ProjectMemberRole.ADMIN && target.getRole() == ProjectMemberRole.ADMIN) {
            throw denied("Administradores não podem gerenciar outros administradores.");
        }
    }

    private ProjectMemberSummaryResponse toSummary(ProjectMember member, Long currentUserId) {
        var user = member.getUser();
        return new ProjectMemberSummaryResponse(
                member.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                member.getRole(),
                member.getJoinedAt(),
                user.getId().equals(currentUserId)
        );
    }

    private ProjectMembershipException denied(String message) {
        return new ProjectMembershipException(HttpStatus.FORBIDDEN, "PROJECT_MEMBER_MANAGEMENT_DENIED", message);
    }

    private ProjectMembershipException invalidRole(String message) {
        return new ProjectMembershipException(HttpStatus.CONFLICT, "PROJECT_OWNER_PROTECTED", message);
    }
}

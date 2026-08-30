package br.com.devtasker.api.project.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.ProjectMemberSummaryResponse;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectMemberQueryService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectMemberQueryService(
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberSummaryResponse> findMembers(
            Long projectId,
            Long userId
    ) {
        projectAccessService.requireMembership(projectId, userId);

        return projectMemberRepository
                .findActiveMembersByProject(projectId)
                .stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (ProjectMember membership) ->
                                                roleOrder(membership.getRole())
                                )
                                .thenComparing(
                                        membership -> membership
                                                .getUser()
                                                .getName(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .thenComparing(ProjectMember::getId)
                )
                .map(membership -> toSummary(membership, userId))
                .toList();
    }

    private int roleOrder(ProjectMemberRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
            case VIEWER -> 3;
        };
    }

    private ProjectMemberSummaryResponse toSummary(
            ProjectMember membership,
            Long currentUserId
    ) {
        var user = membership.getUser();

        return new ProjectMemberSummaryResponse(
                membership.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                membership.getRole(),
                membership.getJoinedAt(),
                user.getId().equals(currentUserId)
        );
    }
}

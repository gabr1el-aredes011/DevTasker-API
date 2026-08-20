package br.com.devtasker.api.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.dto.ProjectDetailsResponse;
import br.com.devtasker.api.project.dto.ProjectSummaryResponse;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectQueryService {

    private final ProjectMemberRepository
            projectMemberRepository;

    private final ProjectAccessService
            projectAccessService;

    public ProjectQueryService(
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectMemberRepository =
                projectMemberRepository;

        this.projectAccessService =
                projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse>
    findProjectsByUser(
            Long userId
    ) {
        return projectMemberRepository
                .findAllByUser_IdOrderByJoinedAtAsc(
                        userId
                )
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailsResponse findById(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                projectAccessService
                        .requireMembership(
                                projectId,
                                userId
                        );

        var project =
                membership.getProject();

        return new ProjectDetailsResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                membership.getRole(),

                project.getOwner().getId(),
                project.getOwner().getName(),

                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public void validateMembership(
            Long projectId,
            Long userId
    ) {
        projectAccessService
                .requireMembership(
                        projectId,
                        userId
                );
    }

    private ProjectSummaryResponse
    toSummaryResponse(
            ProjectMember membership
    ) {
        var project =
                membership.getProject();

        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                membership.getRole(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
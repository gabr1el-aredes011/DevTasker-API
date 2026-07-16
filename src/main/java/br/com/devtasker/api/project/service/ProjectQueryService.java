package br.com.devtasker.api.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.dto.ProjectSummaryResponse;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectQueryService {

    private final ProjectMemberRepository
            projectMemberRepository;

    public ProjectQueryService(
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectMemberRepository =
                projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> findProjectsByUser(
            Long userId
    ) {
        return projectMemberRepository
                .findAllByUser_IdOrderByJoinedAtAsc(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateMembership(
            Long projectId,
            Long userId
    ) {
        projectMemberRepository
                .findByProject_IdAndUser_Id(
                        projectId,
                        userId
                )
                .orElseThrow(ProjectNotFoundException::new);
    }

    private ProjectSummaryResponse toSummaryResponse(
            ProjectMember membership
    ) {
        var project = membership.getProject();

        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                membership.getRole(),
                project.getCreatedAt()
        );
    }
}

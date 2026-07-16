package br.com.devtasker.api.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectMemberRepository =
                projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public ProjectMember requireMembership(
            Long projectId,
            Long userId
    ) {
        return projectMemberRepository
                .findByProject_IdAndUser_Id(
                        projectId,
                        userId
                )
                .orElseThrow(ProjectNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public void requireWriteAccess(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                requireMembership(projectId, userId);

        if (membership.getRole()
                == ProjectMemberRole.VIEWER) {
            throw new ProjectPermissionDeniedException();
        }
    }
}
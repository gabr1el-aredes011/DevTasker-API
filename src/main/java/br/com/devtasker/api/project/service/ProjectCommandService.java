package br.com.devtasker.api.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.CreateProjectRequest;
import br.com.devtasker.api.project.dto.ProjectDetailsResponse;
import br.com.devtasker.api.project.dto.UpdateProjectRequest;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class ProjectCommandService {

    private final ProjectRepository projectRepository;

    private final UserAccountRepository
            userAccountRepository;

    private final ProjectAccessService
            projectAccessService;

    private final ProjectProvisioningService
            projectProvisioningService;

    public ProjectCommandService(
            ProjectRepository projectRepository,
            UserAccountRepository userAccountRepository,
            ProjectAccessService projectAccessService,
            ProjectProvisioningService projectProvisioningService
    ) {
        this.projectRepository =
                projectRepository;

        this.userAccountRepository =
                userAccountRepository;

        this.projectAccessService =
                projectAccessService;

        this.projectProvisioningService =
                projectProvisioningService;
    }

    @Transactional
    public ProjectDetailsResponse create(
            Long userId,
            CreateProjectRequest request
    ) {
        UserAccount owner =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Usuário autenticado não encontrado."
                                )
                        );

        Project project =
                projectRepository.save(
                        Project.create(
                                request.name(),
                                request.description(),
                                owner
                        )
                );

        projectProvisioningService
                .provisionDefaultStructure(
                        project,
                        owner
                );

        return toDetailsResponse(
                project,
                ProjectMemberRole.OWNER
        );
    }

    @Transactional
    public ProjectDetailsResponse update(
            Long projectId,
            Long userId,
            UpdateProjectRequest request
    ) {
        ProjectMember membership =
                projectAccessService
                        .requireManagementAccess(
                                projectId,
                                userId
                        );

        Project project =
                membership.getProject();

        project.updateDetails(
                request.name(),
                request.description()
        );

        Project updatedProject =
                projectRepository
                        .saveAndFlush(project);

        return toDetailsResponse(
                updatedProject,
                membership.getRole()
        );
    }

    @Transactional
    public void archive(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                projectAccessService
                        .requireOwnership(
                                projectId,
                                userId
                        );

        Project project =
                membership.getProject();

        project.archive();

        projectRepository.saveAndFlush(project);
    }

    private ProjectDetailsResponse
    toDetailsResponse(
            Project project,
            ProjectMemberRole membershipRole
    ) {
        return new ProjectDetailsResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                membershipRole,

                project.getOwner().getId(),
                project.getOwner().getName(),

                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}

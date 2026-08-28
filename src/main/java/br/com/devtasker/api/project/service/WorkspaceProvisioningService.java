package br.com.devtasker.api.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class WorkspaceProvisioningService {

    private final ProjectRepository projectRepository;

    private final ProjectProvisioningService
            projectProvisioningService;

    public WorkspaceProvisioningService(
            ProjectRepository projectRepository,
            ProjectProvisioningService projectProvisioningService
    ) {
        this.projectRepository =
                projectRepository;

        this.projectProvisioningService =
                projectProvisioningService;
    }

    @Transactional
    public void createInitialWorkspace(
            UserAccount user
    ) {
        Project project =
                projectRepository.save(
                        Project.createInitial(user)
                );

        projectProvisioningService
                .provisionDefaultStructure(
                        project,
                        user
                );
    }
}
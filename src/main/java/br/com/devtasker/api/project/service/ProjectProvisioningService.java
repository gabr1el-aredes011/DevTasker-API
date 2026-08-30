package br.com.devtasker.api.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.service.BoardWorkflowProvisioningService;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class ProjectProvisioningService {

    private final ProjectMemberRepository projectMemberRepository;
    private final BoardWorkflowProvisioningService boardWorkflowProvisioningService;

    public ProjectProvisioningService(
            ProjectMemberRepository projectMemberRepository,
            BoardWorkflowProvisioningService boardWorkflowProvisioningService
    ) {
        this.projectMemberRepository =
                projectMemberRepository;

        this.boardWorkflowProvisioningService =
                boardWorkflowProvisioningService;
    }

    @Transactional
    public void provisionDefaultStructure(
            Project project,
            UserAccount owner
    ) {
        projectMemberRepository.save(
                ProjectMember.createOwner(
                        project,
                        owner
                )
        );

        boardWorkflowProvisioningService.createDefaultBoard(
                project,
                "Quadro Principal"
        );
    }
}

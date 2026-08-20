package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.CreateProjectRequest;
import br.com.devtasker.api.project.dto.ProjectDetailsResponse;
import br.com.devtasker.api.project.dto.UpdateProjectRequest;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 11L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private ProjectProvisioningService projectProvisioningService;

    @Mock
    private UserAccount owner;

    @Mock
    private ProjectMember membership;

    private ProjectCommandService service;

    @BeforeEach
    void setUp() {
        service = new ProjectCommandService(
                projectRepository,
                userAccountRepository,
                projectAccessService,
                projectProvisioningService
        );
    }

    @Test
    void shouldCreateProjectAndProvisionDefaultStructure() {
        when(userAccountRepository.findById(USER_ID))
                .thenReturn(Optional.of(owner));

        when(owner.getId()).thenReturn(USER_ID);
        when(owner.getName()).thenReturn("Gabriel");

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ProjectDetailsResponse response = service.create(
                USER_ID,
                new CreateProjectRequest(
                        "  DevTasker API  ",
                        "  Backend Java  "
                )
        );

        ArgumentCaptor<Project> projectCaptor =
                ArgumentCaptor.forClass(Project.class);

        verify(projectProvisioningService)
                .provisionDefaultStructure(
                        projectCaptor.capture(),
                        org.mockito.ArgumentMatchers.same(owner)
                );

        Project createdProject = projectCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        "DevTasker API",
                        createdProject.getName()
                ),
                () -> assertEquals(
                        "Backend Java",
                        createdProject.getDescription()
                ),
                () -> assertSame(
                        owner,
                        createdProject.getOwner()
                ),
                () -> assertEquals(
                        ProjectMemberRole.OWNER,
                        response.membershipRole()
                ),
                () -> assertEquals(
                        USER_ID,
                        response.ownerId()
                )
        );
    }

    @Test
    void shouldUpdateProjectWithManagementAccess() {
        Project project = Project.create(
                "Nome anterior",
                null,
                owner
        );

        when(
                projectAccessService
                        .requireManagementAccess(
                                PROJECT_ID,
                                USER_ID
                        )
        ).thenReturn(membership);

        when(membership.getProject())
                .thenReturn(project);

        when(membership.getRole())
                .thenReturn(ProjectMemberRole.ADMIN);

        when(owner.getId()).thenReturn(USER_ID);
        when(owner.getName()).thenReturn("Gabriel");

        when(projectRepository.saveAndFlush(project))
                .thenReturn(project);

        ProjectDetailsResponse response = service.update(
                PROJECT_ID,
                USER_ID,
                new UpdateProjectRequest(
                        "  Novo nome  ",
                        "  Nova descrição  "
                )
        );

        assertAll(
                () -> assertEquals(
                        "Novo nome",
                        response.name()
                ),
                () -> assertEquals(
                        "Nova descrição",
                        response.description()
                ),
                () -> assertEquals(
                        ProjectMemberRole.ADMIN,
                        response.membershipRole()
                )
        );

        verify(projectRepository)
                .saveAndFlush(project);
    }
}

package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.exception.ProjectLabelException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.domain.ProjectLabelColor;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.dto.CreateProjectLabelRequest;
import br.com.devtasker.api.project.dto.UpdateProjectLabelRequest;
import br.com.devtasker.api.project.repository.ProjectLabelRepository;
import br.com.devtasker.api.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ProjectLabelServiceTest {

    private static final Long PROJECT_ID = 7L;
    private static final Long USER_ID = 2L;
    private static final Long LABEL_ID = 11L;

    @Mock private ProjectLabelRepository projectLabelRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectMember membership;
    @Mock private Project project;

    private ProjectLabelService service;

    @BeforeEach
    void setUp() {
        service = new ProjectLabelService(
                projectLabelRepository,
                projectAccessService,
                taskRepository
        );
    }

    @Test
    void shouldCreateLabelForProjectManagers() {
        when(projectAccessService.requireManagementAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getProject()).thenReturn(project);
        when(projectLabelRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ProjectLabel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(
                PROJECT_ID,
                USER_ID,
                new CreateProjectLabelRequest("  Backend  ")
        );

        assertEquals("Backend", response.name());
        assertEquals(ProjectLabelColor.GREEN, response.color());
        verify(projectAccessService).requireManagementAccess(PROJECT_ID, USER_ID);
        verify(projectLabelRepository).countByProject_IdAndArchivedAtIsNull(PROJECT_ID);
    }

    @Test
    void shouldRotateAutomaticColorsAsTheCatalogGrows() {
        when(projectAccessService.requireManagementAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getProject()).thenReturn(project);
        when(projectLabelRepository.countByProject_IdAndArchivedAtIsNull(PROJECT_ID))
                .thenReturn(3L);
        when(projectLabelRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ProjectLabel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(
                PROJECT_ID,
                USER_ID,
                new CreateProjectLabelRequest("Frontend")
        );

        assertEquals(ProjectLabelColor.AMBER, response.color());
    }

    @Test
    void shouldRejectDuplicateActiveNameIgnoringCase() {
        when(projectAccessService.requireManagementAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(projectLabelRepository.existsByProject_IdAndNormalizedNameAndArchivedAtIsNull(
                PROJECT_ID,
                "backend"
        )).thenReturn(true);

        ProjectLabelException exception = assertThrows(
                ProjectLabelException.class,
                () -> service.create(
                        PROJECT_ID,
                        USER_ID,
                        new CreateProjectLabelRequest("BACKEND")
                )
        );

        assertEquals("PROJECT_LABEL_NAME_ALREADY_IN_USE", exception.getErrorCode());
        verify(projectLabelRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldUpdateAndArchiveOnlyLabelsFromTheRequestedProject() {
        ProjectLabel label = ProjectLabel.create(project, "Backend", ProjectLabelColor.BLUE);
        when(projectLabelRepository.findByIdAndProject_IdAndArchivedAtIsNull(LABEL_ID, PROJECT_ID))
                .thenReturn(Optional.of(label));
        when(projectLabelRepository.saveAndFlush(label)).thenReturn(label);

        var response = service.update(
                PROJECT_ID,
                LABEL_ID,
                USER_ID,
                new UpdateProjectLabelRequest("API")
        );
        service.archive(PROJECT_ID, LABEL_ID, USER_ID);

        assertEquals("API", response.name());
        assertEquals(ProjectLabelColor.BLUE, response.color());
        assertEquals(true, label.isArchived());
        verify(projectAccessService, org.mockito.Mockito.times(2))
                .requireManagementAccess(PROJECT_ID, USER_ID);
    }

    @Test
    void shouldExposeUsageCountToEveryProjectMember() {
        ProjectLabel label = ProjectLabel.create(project, "Backend", ProjectLabelColor.BLUE);
        when(projectLabelRepository.findAllByProject_IdAndArchivedAtIsNullOrderByNameAscIdAsc(PROJECT_ID))
                .thenReturn(List.of(label));
        when(taskRepository.countByLabels_IdAndArchivedAtIsNull(label.getId())).thenReturn(3L);

        var response = service.findAll(PROJECT_ID, USER_ID);

        assertEquals(1, response.size());
        assertEquals(3L, response.getFirst().usageCount());
        verify(projectAccessService).requireMembership(PROJECT_ID, USER_ID);
    }
}

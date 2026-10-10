package br.com.devtasker.api.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.board.realtime.BoardRealtimeEvent;
import br.com.devtasker.api.exception.InvalidTaskAssigneeException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.domain.ProjectLabelColor;
import br.com.devtasker.api.project.repository.ProjectLabelRepository;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.task.domain.TaskTechnology;
import br.com.devtasker.api.task.dto.CreateTaskRequest;
import br.com.devtasker.api.task.dto.CreateTaskChecklistItemRequest;
import br.com.devtasker.api.task.dto.UpdateTaskRequest;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long PROJECT_ID = 7L;
    private static final Long COLUMN_ID = 31L;
    private static final Long TASK_ID = 19L;
    private static final Long USER_ID = 2L;
    private static final Long ASSIGNEE_ID = 3L;
    private static final Long SECOND_ASSIGNEE_ID = 4L;

    @Mock private TaskRepository taskRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TaskActivityRecorder activityRecorder;
    @Mock private ProjectLabelRepository projectLabelRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private BoardColumn column;
    @Mock private Board board;
    @Mock private Project project;
    @Mock private UserAccount creator;
    @Mock private UserAccount assignee;
    @Mock private ProjectMember assigneeMembership;
    @Mock private UserAccount secondAssignee;
    @Mock private ProjectMember secondAssigneeMembership;
    @Mock private ProjectLabel backendLabel;
    @Mock private ProjectLabel urgentLabel;
    @Mock private ProjectLabel frontendLabel;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                taskRepository,
                boardColumnRepository,
                boardRepository,
                userAccountRepository,
                projectAccessService,
                projectMemberRepository,
                activityRecorder,
                projectLabelRepository,
                eventPublisher
        );

        when(column.getBoard()).thenReturn(board);
        when(board.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(PROJECT_ID);
        lenient().when(backendLabel.getId()).thenReturn(4L);
        lenient().when(backendLabel.getName()).thenReturn("Backend");
        lenient().when(backendLabel.getColor()).thenReturn(ProjectLabelColor.BLUE);
        lenient().when(urgentLabel.getId()).thenReturn(5L);
        lenient().when(urgentLabel.getName()).thenReturn("Urgente");
        lenient().when(urgentLabel.getColor()).thenReturn(ProjectLabelColor.RED);
        lenient().when(frontendLabel.getId()).thenReturn(6L);
        lenient().when(frontendLabel.getName()).thenReturn("Frontend");
        lenient().when(frontendLabel.getColor()).thenReturn(ProjectLabelColor.VIOLET);
    }

    @Test
    void shouldCreateTaskAssignedToOperationalProjectMember() {
        prepareTaskCreation();
        when(column.getId()).thenReturn(COLUMN_ID);
        when(projectMemberRepository.findActiveMemberships(PROJECT_ID, List.of(ASSIGNEE_ID)))
                .thenReturn(List.of(assigneeMembership));
        when(assigneeMembership.getRole()).thenReturn(ProjectMemberRole.MEMBER);
        when(assigneeMembership.getUser()).thenReturn(assignee);
        when(assignee.getId()).thenReturn(ASSIGNEE_ID);
        when(assignee.getName()).thenReturn("Bianca");
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> {
                    Task task = invocation.getArgument(0);
                    assertEquals(1, task.getAssignees().size());
                    assertSame(assignee, task.getAssignees().getFirst());
                    return task;
                });
        when(projectLabelRepository.findAllByIdInAndProject_IdAndArchivedAtIsNull(
                List.of(4L, 5L), PROJECT_ID
        )).thenReturn(List.of(backendLabel, urgentLabel));

        var response = service.create(
                COLUMN_ID,
                USER_ID,
                new CreateTaskRequest(
                        "Implementar atribuição",
                        null,
                        TaskPriority.HIGH,
                        LocalDate.now().plusDays(2),
                        List.of(ASSIGNEE_ID),
                        List.of(4L, 5L),
                        List.of(TaskTechnology.JAVA, TaskTechnology.ANGULAR)
                )
        );

        assertEquals(ASSIGNEE_ID, response.assignees().getFirst().id());
        assertEquals("Bianca", response.assignees().getFirst().name());
        assertEquals(
                List.of("Backend", "Urgente"),
                response.labels().stream().map(label -> label.name()).toList()
        );
        assertEquals(
                List.of(TaskTechnology.JAVA, TaskTechnology.ANGULAR),
                response.technologies()
        );
        verify(projectAccessService).requireWriteAccess(PROJECT_ID, USER_ID);
        verify(eventPublisher).publishEvent(any(BoardRealtimeEvent.class));
    }

    @Test
    void shouldCreateTaskWithMultipleAssignees() {
        prepareTaskCreation();
        when(column.getId()).thenReturn(COLUMN_ID);
        when(projectMemberRepository.findActiveMemberships(
                PROJECT_ID,
                List.of(ASSIGNEE_ID, SECOND_ASSIGNEE_ID)
        )).thenReturn(List.of(secondAssigneeMembership, assigneeMembership));
        when(assigneeMembership.getRole()).thenReturn(ProjectMemberRole.MEMBER);
        when(assigneeMembership.getUser()).thenReturn(assignee);
        when(assignee.getId()).thenReturn(ASSIGNEE_ID);
        when(assignee.getName()).thenReturn("Bianca");
        when(secondAssigneeMembership.getRole()).thenReturn(ProjectMemberRole.ADMIN);
        when(secondAssigneeMembership.getUser()).thenReturn(secondAssignee);
        when(secondAssignee.getId()).thenReturn(SECOND_ASSIGNEE_ID);
        when(secondAssignee.getName()).thenReturn("Gabriel");
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(
                COLUMN_ID,
                USER_ID,
                new CreateTaskRequest(
                        "Implementar colaboração",
                        null,
                        TaskPriority.HIGH,
                        null,
                        List.of(ASSIGNEE_ID, SECOND_ASSIGNEE_ID),
                        List.of(),
                        List.of()
                )
        );

        assertEquals(
                List.of(ASSIGNEE_ID, SECOND_ASSIGNEE_ID),
                response.assignees().stream().map(user -> user.id()).toList()
        );
    }

    @Test
    void shouldRejectViewerAsTaskAssignee() {
        prepareTaskCreation();
        when(projectMemberRepository.findActiveMemberships(PROJECT_ID, List.of(ASSIGNEE_ID)))
                .thenReturn(List.of(assigneeMembership));
        when(assigneeMembership.getRole()).thenReturn(ProjectMemberRole.VIEWER);

        assertThrows(
                InvalidTaskAssigneeException.class,
                () -> service.create(
                        COLUMN_ID,
                        USER_ID,
                        new CreateTaskRequest(
                                "Tarefa inválida",
                                null,
                                TaskPriority.MEDIUM,
                                null,
                                List.of(ASSIGNEE_ID),
                                List.of(),
                                List.of()
                        )
                )
        );

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectAssigneeOutsideProject() {
        prepareTaskCreation();
        when(projectMemberRepository.findActiveMemberships(PROJECT_ID, List.of(ASSIGNEE_ID)))
                .thenReturn(List.of());

        assertThrows(
                InvalidTaskAssigneeException.class,
                () -> service.create(
                        COLUMN_ID,
                        USER_ID,
                        new CreateTaskRequest(
                                "Tarefa inválida",
                                null,
                                TaskPriority.MEDIUM,
                                null,
                                List.of(ASSIGNEE_ID),
                                List.of(),
                                List.of()
                        )
                )
        );

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldClearTaskAssigneeDuringUpdate() {
        when(column.getId()).thenReturn(COLUMN_ID);
        Task task = Task.create(
                column,
                creator,
                "Tarefa existente",
                null,
                TaskPriority.LOW,
                null,
                0
        );
        task.replaceAssignees(List.of(assignee));

        when(taskRepository.findActiveById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);
        when(projectLabelRepository.findAllByIdInAndProject_IdAndArchivedAtIsNull(
                List.of(6L), PROJECT_ID
        )).thenReturn(List.of(frontendLabel));

        var response = service.update(
                TASK_ID,
                USER_ID,
                new UpdateTaskRequest(
                        "Tarefa atualizada",
                        null,
                        TaskPriority.MEDIUM,
                        null,
                        List.of(),
                        List.of(6L),
                        List.of()
                )
        );

        assertEquals(List.of(), response.assignees());
        assertEquals(List.of(), task.getAssignees());
        assertEquals(List.of("Frontend"), response.labels().stream().map(label -> label.name()).toList());
        verify(projectAccessService).requireWriteAccess(PROJECT_ID, USER_ID);
    }

    @Test
    void shouldAddChecklistItemWithProjectWriteAccess() {
        when(column.getId()).thenReturn(COLUMN_ID);
        Task task = Task.create(
                column,
                creator,
                "Tarefa existente",
                null,
                TaskPriority.LOW,
                null,
                0
        );

        when(taskRepository.findActiveById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        var response = service.addChecklistItem(
                TASK_ID,
                USER_ID,
                new CreateTaskChecklistItemRequest("  Validar publicação  ")
        );

        assertEquals(1, response.checklistItems().size());
        assertEquals("Validar publicação", response.checklistItems().getFirst().title());
        assertEquals(false, response.checklistItems().getFirst().completed());
        verify(projectAccessService).requireWriteAccess(PROJECT_ID, USER_ID);
    }

    private void prepareTaskCreation() {
        when(boardColumnRepository.findByIdAndBoard_ArchivedAtIsNull(COLUMN_ID))
                .thenReturn(Optional.of(column));
        when(userAccountRepository.getReferenceById(USER_ID)).thenReturn(creator);
        when(taskRepository.findMaximumActivePositionByColumnId(COLUMN_ID)).thenReturn(0);
    }
}

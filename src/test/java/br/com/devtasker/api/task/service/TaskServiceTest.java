package br.com.devtasker.api.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.InvalidTaskAssigneeException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.task.dto.CreateTaskRequest;
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

    @Mock private TaskRepository taskRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private BoardColumn column;
    @Mock private Board board;
    @Mock private Project project;
    @Mock private UserAccount creator;
    @Mock private UserAccount assignee;
    @Mock private ProjectMember assigneeMembership;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(
                taskRepository,
                boardColumnRepository,
                boardRepository,
                userAccountRepository,
                projectAccessService,
                projectMemberRepository
        );

        when(column.getBoard()).thenReturn(board);
        when(board.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(PROJECT_ID);
    }

    @Test
    void shouldCreateTaskAssignedToOperationalProjectMember() {
        prepareTaskCreation();
        when(column.getId()).thenReturn(COLUMN_ID);
        when(projectMemberRepository.findActiveMembership(PROJECT_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assigneeMembership));
        when(assigneeMembership.getRole()).thenReturn(ProjectMemberRole.MEMBER);
        when(assigneeMembership.getUser()).thenReturn(assignee);
        when(assignee.getId()).thenReturn(ASSIGNEE_ID);
        when(assignee.getName()).thenReturn("Bianca");
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> {
                    Task task = invocation.getArgument(0);
                    assertSame(assignee, task.getAssignee());
                    return task;
                });

        var response = service.create(
                COLUMN_ID,
                USER_ID,
                new CreateTaskRequest(
                        "Implementar atribuição",
                        null,
                        TaskPriority.HIGH,
                        LocalDate.now().plusDays(2),
                        ASSIGNEE_ID,
                        List.of("Backend", " urgente ", "backend")
                )
        );

        assertEquals(ASSIGNEE_ID, response.assignee().id());
        assertEquals("Bianca", response.assignee().name());
        assertEquals(List.of("Backend", "urgente"), response.labels());
        verify(projectAccessService).requireWriteAccess(PROJECT_ID, USER_ID);
    }

    @Test
    void shouldRejectViewerAsTaskAssignee() {
        prepareTaskCreation();
        when(projectMemberRepository.findActiveMembership(PROJECT_ID, ASSIGNEE_ID))
                .thenReturn(Optional.of(assigneeMembership));
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
                                ASSIGNEE_ID,
                                List.of()
                        )
                )
        );

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectAssigneeOutsideProject() {
        prepareTaskCreation();
        when(projectMemberRepository.findActiveMembership(PROJECT_ID, ASSIGNEE_ID))
                .thenReturn(Optional.empty());

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
                                ASSIGNEE_ID,
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
        task.assignTo(assignee);

        when(taskRepository.findActiveById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(task)).thenReturn(task);

        var response = service.update(
                TASK_ID,
                USER_ID,
                new UpdateTaskRequest(
                        "Tarefa atualizada",
                        null,
                        TaskPriority.MEDIUM,
                        null,
                        null,
                        List.of("Frontend")
                )
        );

        assertNull(response.assignee());
        assertNull(task.getAssignee());
        assertEquals(List.of("Frontend"), response.labels());
        verify(projectAccessService).requireWriteAccess(PROJECT_ID, USER_ID);
    }

    private void prepareTaskCreation() {
        when(boardColumnRepository.findByIdAndBoard_ArchivedAtIsNull(COLUMN_ID))
                .thenReturn(Optional.of(column));
        when(userAccountRepository.getReferenceById(USER_ID)).thenReturn(creator);
        when(taskRepository.findMaximumActivePositionByColumnId(COLUMN_ID)).thenReturn(0);
    }
}

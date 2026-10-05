package br.com.devtasker.api.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.domain.TaskComment;
import br.com.devtasker.api.task.dto.TaskCommentRequest;
import br.com.devtasker.api.task.repository.TaskActivityRepository;
import br.com.devtasker.api.task.repository.TaskCommentRepository;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@ExtendWith(MockitoExtension.class)
class TaskCollaborationServiceTest {

    private static final Long TASK_ID = 19L;
    private static final Long PROJECT_ID = 7L;
    private static final Long USER_ID = 2L;

    @Mock private TaskRepository taskRepository;
    @Mock private TaskCommentRepository commentRepository;
    @Mock private TaskActivityRepository activityRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private TaskActivityRecorder activityRecorder;
    @Mock private Task task;
    @Mock private BoardColumn column;
    @Mock private Board board;
    @Mock private Project project;
    @Mock private ProjectMember membership;
    @Mock private UserAccount user;
    @Mock private UserAccount otherUser;
    @Mock private TaskComment comment;

    private TaskCollaborationService service;

    @BeforeEach
    void setUp() {
        service = new TaskCollaborationService(
                taskRepository,
                commentRepository,
                activityRepository,
                projectAccessService,
                activityRecorder
        );

        lenient().when(taskRepository.findActiveById(TASK_ID)).thenReturn(Optional.of(task));
        lenient().when(task.getId()).thenReturn(TASK_ID);
        lenient().when(task.getColumn()).thenReturn(column);
        lenient().when(column.getBoard()).thenReturn(board);
        lenient().when(board.getProject()).thenReturn(project);
        lenient().when(project.getId()).thenReturn(PROJECT_ID);
        lenient().when(membership.getUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(user.getName()).thenReturn("Gabriel");
        lenient().when(commentRepository.findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(TASK_ID))
                .thenReturn(List.of());
        lenient().when(activityRepository.findTop100ByTask_IdOrderByCreatedAtDescIdDesc(TASK_ID))
                .thenReturn(List.of());
    }

    @Test
    void shouldAllowViewerToReadWithoutWriteControls() {
        when(projectAccessService.requireMembership(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.VIEWER);

        var response = service.findByTask(TASK_ID, USER_ID);

        assertEquals(List.of(), response.comments());
        assertEquals(List.of(), response.activities());
        verify(projectAccessService).requireMembership(PROJECT_ID, USER_ID);
        verify(projectAccessService, never()).requireWriteAccess(any(), any());
    }

    @Test
    void shouldAddCommentForOperationalMemberAndRecordActivity() {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.MEMBER);

        var response = service.addComment(
                TASK_ID,
                USER_ID,
                new TaskCommentRequest("Contexto da entrega.")
        );

        assertEquals(List.of(), response.comments());
        verify(commentRepository).save(any(TaskComment.class));
        verify(activityRecorder).record(
                task,
                user,
                TaskActivityType.COMMENT_ADDED,
                "adicionou um comentário."
        );
        verify(task).recordActivity();
    }

    @Test
    void shouldOnlyAllowAuthorToEditComment() {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(commentRepository.findByIdAndTask_IdAndDeletedAtIsNull(4L, TASK_ID))
                .thenReturn(Optional.of(comment));
        when(comment.getAuthor()).thenReturn(otherUser);
        when(otherUser.getId()).thenReturn(99L);

        assertThrows(
                ProjectPermissionDeniedException.class,
                () -> service.editComment(
                        TASK_ID,
                        4L,
                        USER_ID,
                        new TaskCommentRequest("Alteração indevida.")
                )
        );

        verify(comment, never()).edit(any());
    }

    @Test
    void shouldAllowAdministratorToModerateAnotherUsersComment() {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.ADMIN);
        when(commentRepository.findByIdAndTask_IdAndDeletedAtIsNull(4L, TASK_ID))
                .thenReturn(Optional.of(comment));
        when(comment.getAuthor()).thenReturn(otherUser);
        when(otherUser.getId()).thenReturn(99L);

        service.removeComment(TASK_ID, 4L, USER_ID);

        verify(comment).remove();
        verify(activityRecorder).record(
                task,
                user,
                TaskActivityType.COMMENT_REMOVED,
                "removeu um comentário."
        );
    }
}

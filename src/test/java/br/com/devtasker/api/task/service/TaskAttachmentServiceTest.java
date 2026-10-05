package br.com.devtasker.api.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.exception.TaskAttachmentException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.domain.TaskAttachment;
import br.com.devtasker.api.task.repository.TaskAttachmentRepository;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.task.storage.TaskAttachmentStorage;
import br.com.devtasker.api.user.domain.UserAccount;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentServiceTest {

    private static final Long TASK_ID = 19L;
    private static final Long PROJECT_ID = 7L;
    private static final Long USER_ID = 2L;

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAttachmentRepository attachmentRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private TaskActivityRecorder activityRecorder;
    @Mock private TaskAttachmentStorage storage;
    @Mock private Task task;
    @Mock private BoardColumn column;
    @Mock private Board board;
    @Mock private Project project;
    @Mock private ProjectMember membership;
    @Mock private UserAccount user;
    @Mock private UserAccount otherUser;
    @Mock private TaskAttachment attachment;

    private TaskAttachmentService service;

    @BeforeEach
    void setUp() {
        service = new TaskAttachmentService(
                taskRepository,
                attachmentRepository,
                projectAccessService,
                activityRecorder,
                storage
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
        lenient().when(attachmentRepository
                .findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(TASK_ID))
                .thenReturn(List.of());
    }

    @Test
    void shouldAllowViewerToListAttachmentsWithoutDeletePermission() {
        when(projectAccessService.requireMembership(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.VIEWER);
        when(attachmentRepository
                .findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(TASK_ID))
                .thenReturn(List.of(attachment));
        when(attachment.getUploader()).thenReturn(otherUser);
        when(otherUser.getId()).thenReturn(99L);
        when(otherUser.getName()).thenReturn("Outro usuário");

        var response = service.findAll(TASK_ID, USER_ID);

        assertEquals(1, response.size());
        assertFalse(response.getFirst().canDelete());
        verify(projectAccessService, never()).requireWriteAccess(any(), any());
    }

    @Test
    void shouldUploadAllowedFileAndRecordActivity() throws IOException {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(attachmentRepository.countByTask_IdAndDeletedAtIsNull(TASK_ID)).thenReturn(0L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.pdf",
                "application/pdf",
                "conteudo".getBytes()
        );

        service.upload(TASK_ID, USER_ID, file);

        verify(storage).store(any(), any());
        verify(attachmentRepository).saveAndFlush(any(TaskAttachment.class));
        verify(activityRecorder).record(
                task,
                user,
                TaskActivityType.ATTACHMENT_ADDED,
                "anexou o arquivo \"evidencia.pdf\"."
        );
    }

    @Test
    void shouldRejectUnsupportedFileType() throws IOException {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(attachmentRepository.countByTask_IdAndDeletedAtIsNull(TASK_ID)).thenReturn(0L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "conteudo".getBytes()
        );

        assertThrows(
                TaskAttachmentException.class,
                () -> service.upload(TASK_ID, USER_ID, file)
        );

        verify(storage, never()).store(any(), any());
    }

    @Test
    void shouldPreventMemberFromDeletingAnotherUsersAttachment() {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.MEMBER);
        when(attachmentRepository.findByIdAndTask_IdAndDeletedAtIsNull(5L, TASK_ID))
                .thenReturn(Optional.of(attachment));
        when(attachment.getUploader()).thenReturn(otherUser);
        when(otherUser.getId()).thenReturn(99L);

        assertThrows(
                ProjectPermissionDeniedException.class,
                () -> service.remove(TASK_ID, 5L, USER_ID)
        );

        verify(attachment, never()).remove();
    }

    @Test
    void shouldAllowAdministratorToDeleteAnotherUsersAttachment() throws IOException {
        when(projectAccessService.requireWriteAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getRole()).thenReturn(ProjectMemberRole.ADMIN);
        when(attachmentRepository.findByIdAndTask_IdAndDeletedAtIsNull(5L, TASK_ID))
                .thenReturn(Optional.of(attachment));
        when(attachment.getUploader()).thenReturn(otherUser);
        when(otherUser.getId()).thenReturn(99L);
        when(attachment.getOriginalFileName()).thenReturn("evidencia.pdf");
        when(attachment.getStorageKey()).thenReturn("storage-key");

        service.remove(TASK_ID, 5L, USER_ID);

        verify(attachment).remove();
        verify(storage).delete("storage-key");
        verify(activityRecorder).record(
                task,
                user,
                TaskActivityType.ATTACHMENT_REMOVED,
                "removeu o anexo \"evidencia.pdf\"."
        );
    }
}

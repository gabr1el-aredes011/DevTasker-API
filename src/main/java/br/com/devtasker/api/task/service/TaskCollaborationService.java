package br.com.devtasker.api.task.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.exception.TaskCommentNotFoundException;
import br.com.devtasker.api.exception.TaskNotFoundException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivity;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.domain.TaskComment;
import br.com.devtasker.api.task.dto.TaskActivityResponse;
import br.com.devtasker.api.task.dto.TaskCollaborationResponse;
import br.com.devtasker.api.task.dto.TaskCommentRequest;
import br.com.devtasker.api.task.dto.TaskCommentResponse;
import br.com.devtasker.api.task.dto.TaskUserSummaryResponse;
import br.com.devtasker.api.task.repository.TaskActivityRepository;
import br.com.devtasker.api.task.repository.TaskCommentRepository;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class TaskCollaborationService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskActivityRepository activityRepository;
    private final ProjectAccessService projectAccessService;
    private final TaskActivityRecorder activityRecorder;

    public TaskCollaborationService(
            TaskRepository taskRepository,
            TaskCommentRepository commentRepository,
            TaskActivityRepository activityRepository,
            ProjectAccessService projectAccessService,
            TaskActivityRecorder activityRecorder
    ) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
        this.projectAccessService = projectAccessService;
        this.activityRecorder = activityRecorder;
    }

    @Transactional(readOnly = true)
    public TaskCollaborationResponse findByTask(Long taskId, Long userId) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireMembership(
                projectId(task),
                userId
        );

        return toResponse(task, membership);
    }

    @Transactional
    public TaskCollaborationResponse addComment(
            Long taskId,
            Long userId,
            TaskCommentRequest request
    ) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireWriteAccess(
                projectId(task),
                userId
        );
        UserAccount author = membership.getUser();

        commentRepository.save(TaskComment.create(task, author, request.content()));
        activityRecorder.record(
                task,
                author,
                TaskActivityType.COMMENT_ADDED,
                "adicionou um comentário."
        );
        task.recordActivity();
        taskRepository.save(task);

        return toResponse(task, membership);
    }

    @Transactional
    public TaskCollaborationResponse editComment(
            Long taskId,
            Long commentId,
            Long userId,
            TaskCommentRequest request
    ) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireWriteAccess(
                projectId(task),
                userId
        );
        TaskComment comment = findComment(taskId, commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ProjectPermissionDeniedException();
        }

        comment.edit(request.content());
        activityRecorder.record(
                task,
                membership.getUser(),
                TaskActivityType.COMMENT_EDITED,
                "editou um comentário."
        );
        task.recordActivity();
        taskRepository.save(task);

        return toResponse(task, membership);
    }

    @Transactional
    public TaskCollaborationResponse removeComment(
            Long taskId,
            Long commentId,
            Long userId
    ) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireWriteAccess(
                projectId(task),
                userId
        );
        TaskComment comment = findComment(taskId, commentId);

        boolean ownsComment = comment.getAuthor().getId().equals(userId);
        boolean canModerate = membership.getRole() == ProjectMemberRole.OWNER
                || membership.getRole() == ProjectMemberRole.ADMIN;

        if (!ownsComment && !canModerate) {
            throw new ProjectPermissionDeniedException();
        }

        comment.remove();
        activityRecorder.record(
                task,
                membership.getUser(),
                TaskActivityType.COMMENT_REMOVED,
                "removeu um comentário."
        );
        task.recordActivity();
        taskRepository.save(task);

        return toResponse(task, membership);
    }

    private TaskCollaborationResponse toResponse(Task task, ProjectMember membership) {
        Long userId = membership.getUser().getId();
        boolean canWrite = membership.getRole() != ProjectMemberRole.VIEWER;
        boolean canModerate = membership.getRole() == ProjectMemberRole.OWNER
                || membership.getRole() == ProjectMemberRole.ADMIN;

        return new TaskCollaborationResponse(
                commentRepository
                        .findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(task.getId())
                        .stream()
                        .map(comment -> toCommentResponse(
                                comment,
                                canWrite && comment.getAuthor().getId().equals(userId),
                                canWrite && (canModerate || comment.getAuthor().getId().equals(userId))
                        ))
                        .toList(),
                activityRepository
                        .findTop100ByTask_IdOrderByCreatedAtDescIdDesc(task.getId())
                        .stream()
                        .map(this::toActivityResponse)
                        .toList()
        );
    }

    private TaskCommentResponse toCommentResponse(
            TaskComment comment,
            boolean canEdit,
            boolean canDelete
    ) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getContent(),
                toUserResponse(comment.getAuthor()),
                canEdit,
                canDelete,
                comment.getEditedAt() != null,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private TaskActivityResponse toActivityResponse(TaskActivity activity) {
        return new TaskActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getDescription(),
                toUserResponse(activity.getActor()),
                activity.getCreatedAt()
        );
    }

    private TaskUserSummaryResponse toUserResponse(UserAccount user) {
        return new TaskUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }

    private Task findActiveTask(Long taskId) {
        return taskRepository.findActiveById(taskId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private TaskComment findComment(Long taskId, Long commentId) {
        return commentRepository.findByIdAndTask_IdAndDeletedAtIsNull(commentId, taskId)
                .orElseThrow(TaskCommentNotFoundException::new);
    }

    private Long projectId(Task task) {
        return task.getColumn().getBoard().getProject().getId();
    }
}

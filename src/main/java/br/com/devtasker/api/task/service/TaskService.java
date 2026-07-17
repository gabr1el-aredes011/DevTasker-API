package br.com.devtasker.api.task.service;

import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.exception.BoardColumnNotFoundException;
import br.com.devtasker.api.exception.TaskNotFoundException;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.dto.CreateTaskRequest;
import br.com.devtasker.api.task.dto.TaskResponse;
import br.com.devtasker.api.task.dto.TaskUserSummaryResponse;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;
import br.com.devtasker.api.task.dto.UpdateTaskRequest;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProjectAccessService projectAccessService;

    public TaskService(
            TaskRepository taskRepository,
            BoardColumnRepository boardColumnRepository,
            UserAccountRepository userAccountRepository,
            ProjectAccessService projectAccessService
    ) {
        this.taskRepository = taskRepository;
        this.boardColumnRepository =
                boardColumnRepository;
        this.userAccountRepository =
                userAccountRepository;
        this.projectAccessService =
                projectAccessService;
    }

    @Transactional
    public TaskResponse create(
            Long columnId,
            Long userId,
            CreateTaskRequest request
    ) {
        BoardColumn column = findColumn(columnId);

        Long projectId = column
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireWriteAccess(
                projectId,
                userId
        );

        UserAccount creator =
                userAccountRepository.getReferenceById(userId);

        Integer maximumPosition =
                taskRepository
                        .findMaximumActivePositionByColumnId(
                                columnId
                        );

        Task task = Task.create(
                column,
                creator,
                request.title().trim(),
                normalizeDescription(request.description()),
                request.priority(),
                request.dueDate(),
                maximumPosition + 1
        );

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAllByColumn(
            Long columnId,
            Long userId
    ) {
        BoardColumn column = findColumn(columnId);

        Long projectId = column
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireMembership(
                projectId,
                userId
        );

        return taskRepository
                .findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
                        columnId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(
            Long taskId,
            Long userId
    ) {
        Task task = findActiveTask(taskId);

        Long projectId = task
                .getColumn()
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireMembership(
                projectId,
                userId
        );

        return toResponse(task);
    }

    private BoardColumn findColumn(Long columnId) {
        return boardColumnRepository
                .findById(columnId)
                .orElseThrow(
                        BoardColumnNotFoundException::new
                );
    }

    private String normalizeDescription(
            String description
    ) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getColumn().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),
                toUserResponse(task.getCreator()),
                toUserResponse(task.getAssignee()),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private TaskUserSummaryResponse toUserResponse(
            UserAccount user
    ) {
        if (user == null) {
            return null;
        }

        return new TaskUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }
    
    @Transactional
    public TaskResponse update(
            Long taskId,
            Long userId,
            UpdateTaskRequest request
    ) {
        Task task = findActiveTask(taskId);

        Long projectId = task
                .getColumn()
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireWriteAccess(
                projectId,
                userId
        );

        task.updateDetails(
                request.title(),
                normalizeDescription(request.description()),
                request.priority(),
                request.dueDate()
        );

        Task updatedTask =
                taskRepository.saveAndFlush(task);

        return toResponse(updatedTask);
    }

    @Transactional
    public void archive(
            Long taskId,
            Long userId
    ) {
        Task task = findActiveTask(taskId);

        Long projectId = task
                .getColumn()
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireWriteAccess(
                projectId,
                userId
        );

        task.archive();

        taskRepository.save(task);
    }
    
    private Task findActiveTask(Long taskId) {
        return taskRepository
                .findByIdAndArchivedAtIsNull(taskId)
                .orElseThrow(TaskNotFoundException::new);
    }
}
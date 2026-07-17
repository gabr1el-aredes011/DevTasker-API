package br.com.devtasker.api.task.service;

import java.util.List;
import java.util.ArrayList;

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


import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.InvalidTaskMoveException;
import br.com.devtasker.api.task.dto.MoveTaskRequest;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProjectAccessService projectAccessService;
    private final BoardRepository boardRepository;

    public TaskService(
            TaskRepository taskRepository,
            BoardColumnRepository boardColumnRepository,
            BoardRepository boardRepository,
            UserAccountRepository userAccountRepository,
            ProjectAccessService projectAccessService
    ) {
        this.taskRepository = taskRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.boardRepository = boardRepository;
        this.userAccountRepository = userAccountRepository;
        this.projectAccessService = projectAccessService;
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
    @Transactional
    public TaskResponse move(
            Long taskId,
            Long userId,
            MoveTaskRequest request
    ) {
        Long boardId = taskRepository
                .findBoardIdByActiveTaskId(taskId)
                .orElseThrow(TaskNotFoundException::new);

        BoardColumn targetColumn = findColumn(
                request.targetColumnId()
        );

        Long targetBoardId = targetColumn
                .getBoard()
                .getId();

        if (!boardId.equals(targetBoardId)) {
            throw new InvalidTaskMoveException(
                    "A tarefa só pode ser movida entre colunas do mesmo quadro."
            );
        }

        Long projectId = targetColumn
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireWriteAccess(
                projectId,
                userId
        );

        boardRepository
                .findByIdForUpdate(boardId)
                .orElseThrow(BoardNotFoundException::new);

        Task task = findActiveTask(taskId);

        Long sourceColumnId = task
                .getColumn()
                .getId();

        if (sourceColumnId.equals(targetColumn.getId())) {
            moveInsideSameColumn(
                    task,
                    targetColumn,
                    request.targetPosition()
            );
        } else {
            moveBetweenColumns(
                    task,
                    targetColumn,
                    request.targetPosition()
            );
        }

        return toResponse(task);
    }
    private void moveInsideSameColumn(
            Task task,
            BoardColumn column,
            Integer targetPosition
    ) {
        List<Task> tasks = new ArrayList<>(
                taskRepository
                        .findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
                                column.getId()
                        )
        );

        boolean removed = tasks.removeIf(
                currentTask ->
                        currentTask.getId().equals(task.getId())
        );

        if (!removed) {
            throw new TaskNotFoundException();
        }

        validateTargetPosition(
                targetPosition,
                tasks.size()
        );

        // Recoloca a tarefa na posição solicitada.
        tasks.add(targetPosition, task);

        // Primeiro libera as posições 0, 1, 2...
        moveToTemporaryPositions(tasks, column);
        taskRepository.flush();

        // Depois aplica a ordem final.
        applyFinalPositions(tasks, column);
        taskRepository.flush();
    }

    private void moveBetweenColumns(
            Task task,
            BoardColumn targetColumn,
            Integer targetPosition
    ) {
        BoardColumn sourceColumn = task.getColumn();

        List<Task> sourceTasks = new ArrayList<>(
                taskRepository
                        .findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
                                sourceColumn.getId()
                        )
        );

        List<Task> targetTasks = new ArrayList<>(
                taskRepository
                        .findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
                                targetColumn.getId()
                        )
        );

        boolean removed = sourceTasks.removeIf(
                currentTask ->
                        currentTask.getId().equals(task.getId())
        );

        if (!removed) {
            throw new TaskNotFoundException();
        }

        validateTargetPosition(
                targetPosition,
                targetTasks.size()
        );

        /*
         * Calculamos posições temporárias altas para evitar conflito
         * com a restrição UNIQUE(column_id, position).
         */
        int sourceTemporaryBase = calculateTemporaryBase(
                sourceTasks,
                task.getPosition()
        );

        int targetTemporaryBase = calculateTemporaryBase(
                targetTasks,
                null
        );

        applyTemporaryPositions(
                sourceTasks,
                sourceColumn,
                sourceTemporaryBase
        );

        applyTemporaryPositions(
                targetTasks,
                targetColumn,
                targetTemporaryBase
        );

        taskRepository.flush();

        /*
         * Move a tarefa para a coluna de destino usando primeiro
         * uma posição temporária ainda não ocupada.
         */
        int movingTaskTemporaryPosition =
                targetTemporaryBase + targetTasks.size();

        task.relocate(
                targetColumn,
                movingTaskTemporaryPosition
        );

        taskRepository.flush();

        // Insere a tarefa na posição desejada da lista de destino.
        targetTasks.add(targetPosition, task);

        // Normaliza as duas colunas para 0, 1, 2, 3...
        applyFinalPositions(
                sourceTasks,
                sourceColumn
        );

        applyFinalPositions(
                targetTasks,
                targetColumn
        );

        taskRepository.flush();
    }

    private void validateTargetPosition(
            Integer targetPosition,
            int maximumAllowedPosition
    ) {
        if (targetPosition == null
                || targetPosition < 0
                || targetPosition > maximumAllowedPosition) {

            throw new InvalidTaskMoveException(
                    "A posição de destino é inválida para a coluna informada."
            );
        }
    }

    private void moveToTemporaryPositions(
            List<Task> tasks,
            BoardColumn column
    ) {
        int temporaryBase =
                calculateTemporaryBase(tasks, null);

        applyTemporaryPositions(
                tasks,
                column,
                temporaryBase
        );
    }

    private int calculateTemporaryBase(
            List<Task> tasks,
            Integer additionalPosition
    ) {
        int maximumPosition =
                additionalPosition == null
                        ? -1
                        : additionalPosition;

        for (Task task : tasks) {
            maximumPosition = Math.max(
                    maximumPosition,
                    task.getPosition()
            );
        }

        /*
         * O valor 1000 cria distância suficiente das posições
         * normais da coluna.
         */
        return maximumPosition + tasks.size() + 1000;
    }

    private void applyTemporaryPositions(
            List<Task> tasks,
            BoardColumn column,
            int temporaryBase
    ) {
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).relocate(
                    column,
                    temporaryBase + index
            );
        }
    }

    private void applyFinalPositions(
            List<Task> tasks,
            BoardColumn column
    ) {
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).relocate(
                    column,
                    index
            );
        }
    }

}
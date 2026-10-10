package br.com.devtasker.api.task.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.board.realtime.BoardRealtimeEvent;
import br.com.devtasker.api.board.realtime.BoardRealtimeEventType;
import br.com.devtasker.api.exception.BoardColumnNotFoundException;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.InvalidTaskAssigneeException;
import br.com.devtasker.api.exception.InvalidTaskMoveException;
import br.com.devtasker.api.exception.ProjectLabelException;
import br.com.devtasker.api.exception.TaskNotFoundException;
import br.com.devtasker.api.exception.TaskChecklistItemNotFoundException;
import br.com.devtasker.api.exception.TaskChecklistLimitException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.repository.ProjectLabelRepository;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.domain.TaskChecklistItem;
import br.com.devtasker.api.task.dto.CreateTaskRequest;
import br.com.devtasker.api.task.dto.CreateTaskChecklistItemRequest;
import br.com.devtasker.api.task.dto.MoveTaskRequest;
import br.com.devtasker.api.task.dto.TaskResponse;
import br.com.devtasker.api.task.dto.TaskChecklistItemResponse;
import br.com.devtasker.api.task.dto.TaskUserSummaryResponse;
import br.com.devtasker.api.task.dto.TaskLabelResponse;
import br.com.devtasker.api.task.dto.UpdateTaskRequest;
import br.com.devtasker.api.task.dto.UpdateTaskChecklistItemRequest;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProjectAccessService projectAccessService;
    private final BoardRepository boardRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskActivityRecorder activityRecorder;
    private final ProjectLabelRepository projectLabelRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(
            TaskRepository taskRepository,
            BoardColumnRepository boardColumnRepository,
            BoardRepository boardRepository,
            UserAccountRepository userAccountRepository,
            ProjectAccessService projectAccessService,
            ProjectMemberRepository projectMemberRepository,
            TaskActivityRecorder activityRecorder,
            ProjectLabelRepository projectLabelRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.boardRepository = boardRepository;
        this.userAccountRepository = userAccountRepository;
        this.projectAccessService = projectAccessService;
        this.projectMemberRepository = projectMemberRepository;
        this.activityRecorder = activityRecorder;
        this.projectLabelRepository = projectLabelRepository;
        this.eventPublisher = eventPublisher;
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

        task.replaceAssignees(resolveAssignees(projectId, request.assigneeIds()));
        task.replaceLabels(resolveLabels(projectId, request.labelIds()));
        task.replaceTechnologies(request.technologies());

        Task createdTask = taskRepository.save(task);
        activityRecorder.record(
                createdTask,
                creator,
                TaskActivityType.TASK_CREATED,
                "criou a tarefa."
        );
        publishRealtimeEvent(createdTask, userId, BoardRealtimeEventType.TASK_CREATED);

        return toResponse(createdTask);
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
                .findByIdAndBoard_ArchivedAtIsNull(
                        columnId
                )
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
                task.getAssignees().stream()
                        .map(this::toUserResponse)
                        .toList(),
                task.getLabels().stream()
                        .map(this::toLabelResponse)
                        .toList(),
                task.getTechnologies(),
                task.getChecklistItems().stream()
                        .map(this::toChecklistItemResponse)
                        .toList(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private TaskChecklistItemResponse toChecklistItemResponse(
            TaskChecklistItem item
    ) {
        return new TaskChecklistItemResponse(
                item.getId(),
                item.getTitle(),
                item.isCompleted(),
                item.getPosition(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private TaskLabelResponse toLabelResponse(ProjectLabel label) {
        return new TaskLabelResponse(
                label.getId(),
                label.getName(),
                label.getColor(),
                label.isArchived()
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

        task.replaceAssignees(resolveAssignees(projectId, request.assigneeIds()));

        List<ProjectLabel> nextLabels = new ArrayList<>(
                resolveLabels(projectId, request.labelIds())
        );
        task.getLabels().stream()
                .filter(ProjectLabel::isArchived)
                .filter(label -> !nextLabels.contains(label))
                .forEach(nextLabels::add);

        if (nextLabels.size() > Task.MAXIMUM_LABELS) {
            throw invalidLabels(
                    "Remova uma label ativa antes de adicionar outra; labels arquivadas ainda ocupam o limite da tarefa."
            );
        }
        task.replaceLabels(nextLabels);
        task.replaceTechnologies(request.technologies());

        Task updatedTask =
                taskRepository.saveAndFlush(task);

        activityRecorder.record(
                updatedTask,
                userId,
                TaskActivityType.TASK_UPDATED,
                "atualizou os detalhes da tarefa."
        );
        publishRealtimeEvent(updatedTask, userId, BoardRealtimeEventType.TASK_UPDATED);

        return toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse addChecklistItem(
            Long taskId,
            Long userId,
            CreateTaskChecklistItemRequest request
    ) {
        Task task = findActiveTask(taskId);
        requireTaskWriteAccess(task, userId);

        if (task.getChecklistItems().size() >= 50) {
            throw new TaskChecklistLimitException();
        }

        TaskChecklistItem item = task.addChecklistItem(request.title());
        task.recordActivity();

        Task updatedTask = taskRepository.saveAndFlush(task);
        activityRecorder.record(
                updatedTask,
                userId,
                TaskActivityType.CHECKLIST_ITEM_ADDED,
                "adicionou o item \"" + item.getTitle() + "\" à checklist."
        );

        return toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateChecklistItem(
            Long taskId,
            Long itemId,
            Long userId,
            UpdateTaskChecklistItemRequest request
    ) {
        Task task = findActiveTask(taskId);
        requireTaskWriteAccess(task, userId);

        TaskChecklistItem item = task.findChecklistItem(itemId)
                .orElseThrow(TaskChecklistItemNotFoundException::new);

        boolean wasCompleted = item.isCompleted();
        item.update(request.title(), request.completed());
        task.recordActivity();

        Task updatedTask = taskRepository.saveAndFlush(task);
        String activityDescription = wasCompleted == item.isCompleted()
                ? "atualizou o item \"" + item.getTitle() + "\" da checklist."
                : item.isCompleted()
                        ? "concluiu o item \"" + item.getTitle() + "\" da checklist."
                        : "reabriu o item \"" + item.getTitle() + "\" da checklist.";

        activityRecorder.record(
                updatedTask,
                userId,
                TaskActivityType.CHECKLIST_ITEM_UPDATED,
                activityDescription
        );

        return toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse removeChecklistItem(
            Long taskId,
            Long itemId,
            Long userId
    ) {
        Task task = findActiveTask(taskId);
        requireTaskWriteAccess(task, userId);

        TaskChecklistItem item = task.findChecklistItem(itemId)
                .orElseThrow(TaskChecklistItemNotFoundException::new);

        String itemTitle = item.getTitle();
        task.removeChecklistItem(item);
        task.recordActivity();

        Task updatedTask = taskRepository.saveAndFlush(task);
        activityRecorder.record(
                updatedTask,
                userId,
                TaskActivityType.CHECKLIST_ITEM_REMOVED,
                "removeu o item \"" + itemTitle + "\" da checklist."
        );

        return toResponse(updatedTask);
    }

    private void requireTaskWriteAccess(Task task, Long userId) {
        Long projectId = task
                .getColumn()
                .getBoard()
                .getProject()
                .getId();

        projectAccessService.requireWriteAccess(projectId, userId);
    }

    @Transactional
    public void archive(
        Long taskId,
        Long userId
    ) {
        Task task = findActiveTask(taskId);

        BoardColumn column = task.getColumn();

        Long projectId = column
            .getBoard()
            .getProject()
            .getId();

        projectAccessService.requireWriteAccess(
            projectId,
            userId
        );

        task.archive();

        taskRepository.saveAndFlush(task);
        activityRecorder.record(
                task,
                userId,
                TaskActivityType.TASK_ARCHIVED,
                "arquivou a tarefa."
        );
        publishRealtimeEvent(task, userId, BoardRealtimeEventType.TASK_ARCHIVED);

        List<Task> remainingTasks =
            taskRepository
                .findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
                    column.getId()
                );

       
        moveToTemporaryPositions(
            remainingTasks,
            column
        );

        taskRepository.flush();

        applyFinalPositions(
            remainingTasks,
            column
        );

        taskRepository.flush();
    }
    
    private Task findActiveTask(Long taskId) {
        return taskRepository
                .findActiveById(taskId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private List<UserAccount> resolveAssignees(
            Long projectId,
            List<Long> requestedAssigneeIds
    ) {
        if (requestedAssigneeIds == null || requestedAssigneeIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> assigneeIds = new LinkedHashSet<>(requestedAssigneeIds);
        if (assigneeIds.contains(null)
                || assigneeIds.size() != requestedAssigneeIds.size()
                || assigneeIds.size() > Task.MAXIMUM_ASSIGNEES) {
            throw new InvalidTaskAssigneeException();
        }

        List<ProjectMember> memberships = projectMemberRepository.findActiveMemberships(
                projectId,
                List.copyOf(assigneeIds)
        );

        if (memberships.size() != assigneeIds.size()
                || memberships.stream().anyMatch(
                        membership -> membership.getRole() == ProjectMemberRole.VIEWER
                )) {
            throw new InvalidTaskAssigneeException();
        }

        Map<Long, ProjectMember> membershipsByUserId = memberships.stream()
                .collect(Collectors.toMap(
                        membership -> membership.getUser().getId(),
                        Function.identity()
                ));

        if (membershipsByUserId.size() != assigneeIds.size()) {
            throw new InvalidTaskAssigneeException();
        }

        return assigneeIds.stream()
                .map(userId -> membershipsByUserId.get(userId).getUser())
                .toList();
    }

    private List<ProjectLabel> resolveLabels(
            Long projectId,
            List<Long> requestedLabelIds
    ) {
        if (requestedLabelIds == null || requestedLabelIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> labelIds = new LinkedHashSet<>(requestedLabelIds);
        if (labelIds.contains(null) || labelIds.size() != requestedLabelIds.size()) {
            throw invalidLabels("As labels da tarefa devem possuir identificadores únicos e válidos.");
        }

        if (labelIds.size() > Task.MAXIMUM_LABELS) {
            throw invalidLabels("Uma tarefa pode possuir no máximo 5 labels.");
        }

        List<ProjectLabel> labels = projectLabelRepository
                .findAllByIdInAndProject_IdAndArchivedAtIsNull(
                        List.copyOf(labelIds),
                        projectId
                );

        Map<Long, ProjectLabel> labelsById = labels.stream()
                .collect(Collectors.toMap(ProjectLabel::getId, Function.identity()));

        if (labelsById.size() != labelIds.size()) {
            throw invalidLabels("Uma ou mais labels não pertencem ao projeto ou estão arquivadas.");
        }

        return labelIds.stream()
                .map(labelsById::get)
                .toList();
    }

    private ProjectLabelException invalidLabels(String message) {
        return new ProjectLabelException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TASK_LABELS",
                message
        );
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
                .findActiveByIdForUpdate(boardId)
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

        task.recordActivity();
        activityRecorder.record(
                task,
                userId,
                TaskActivityType.TASK_MOVED,
                "moveu a tarefa para \"" + targetColumn.getName() + "\"."
        );
        publishRealtimeEvent(task, userId, BoardRealtimeEventType.TASK_MOVED);

        return toResponse(task);
    }

    private void publishRealtimeEvent(
            Task task,
            Long actorUserId,
            BoardRealtimeEventType type
    ) {
        eventPublisher.publishEvent(
                BoardRealtimeEvent.taskChanged(
                        task.getColumn().getBoard().getId(),
                        task.getId(),
                        actorUserId,
                        type
                )
        );
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

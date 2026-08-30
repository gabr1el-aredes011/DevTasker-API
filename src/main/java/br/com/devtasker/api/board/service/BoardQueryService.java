package br.com.devtasker.api.board.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.dto.BoardColumnResponse;
import br.com.devtasker.api.board.dto.BoardDetailsResponse;
import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.dto.KanbanBoardResponse;
import br.com.devtasker.api.board.dto.KanbanColumnResponse;
import br.com.devtasker.api.board.dto.KanbanTaskResponse;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.repository.TaskRepository;

@Service
public class BoardQueryService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository
            projectMemberRepository;

    public BoardQueryService(
            BoardRepository boardRepository,
            BoardColumnRepository boardColumnRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository
    ) {
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<BoardSummaryResponse> findBoardsByProject(
            Long projectId,
            Long userId
    ) {
        validateProjectMembership(projectId, userId);

        return boardRepository
                .findAllByProject_IdAndArchivedAtIsNullOrderByIdAsc(projectId)
                .stream()
                .map(board -> new BoardSummaryResponse(
                        board.getId(),
                        board.getProject().getId(),
                        board.getName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardDetailsResponse findBoardById(
            Long boardId,
            Long userId
    ) {
        Board board = boardRepository
                .findByIdAndArchivedAtIsNull(boardId)
                .orElseThrow(BoardNotFoundException::new);

        Long projectId = board
                .getProject()
                .getId();

        if (!projectMemberRepository
                .existsActiveMembership(
                        projectId,
                        userId
                )) {
            throw new BoardNotFoundException();
        }

        List<BoardColumnResponse> columns =
                boardColumnRepository
                        .findAllByBoard_IdOrderByPositionAsc(
                                boardId
                        )
                        .stream()
                        .map(this::toColumnResponse)
                        .toList();

        return new BoardDetailsResponse(
                board.getId(),
                projectId,
                board.getName(),
                columns
        );
    }

    private void validateProjectMembership(
            Long projectId,
            Long userId
    ) {
        if (!projectMemberRepository
                .existsActiveMembership(
                        projectId,
                        userId
                )) {
            throw new ProjectNotFoundException();
        }
    }

    private BoardColumnResponse toColumnResponse(
            BoardColumn column
    ) {
        return new BoardColumnResponse(
                column.getId(),
                column.getName(),
                column.getCategory(),
                column.getPosition()
        );
    }
    @Transactional(readOnly = true)
    public KanbanBoardResponse findKanbanByBoardId(
            Long boardId,
            Long userId
    ) {
        Board board = boardRepository
                .findByIdAndArchivedAtIsNull(boardId)
                .orElseThrow(BoardNotFoundException::new);

        Long projectId = board
                .getProject()
                .getId();

        boolean hasAccess = projectMemberRepository
                .existsActiveMembership(
                        projectId,
                        userId
                );

        if (!hasAccess) {
            throw new BoardNotFoundException();
        }

        var columns = boardColumnRepository
                .findAllByBoard_IdOrderByPositionAsc(boardId);

        var tasks = taskRepository
                .findAllActiveByBoardId(boardId);

        Map<Long, List<KanbanTaskResponse>> tasksByColumn =
                groupTasksByColumn(tasks);

        List<KanbanColumnResponse> columnResponses =
                columns.stream()
                        .map(column ->
                                new KanbanColumnResponse(
                                        column.getId(),
                                        column.getName(),
                                        column.getCategory(),
                                        column.getPosition(),
                                        tasksByColumn.getOrDefault(
                                                column.getId(),
                                                List.of()
                                        )
                                )
                        )
                        .toList();

        return new KanbanBoardResponse(
                board.getId(),
                projectId,
                board.getName(),
                columnResponses
        );
    }
    private Map<Long, List<KanbanTaskResponse>>
    groupTasksByColumn(List<Task> tasks) {

        Map<Long, List<KanbanTaskResponse>> groupedTasks =
                new HashMap<>();

        for (Task task : tasks) {
            Long columnId = task
                    .getColumn()
                    .getId();

            groupedTasks
                    .computeIfAbsent(
                            columnId,
                            ignored -> new ArrayList<>()
                    )
                    .add(toKanbanTaskResponse(task));
        }

        return groupedTasks;
    }
    
    private KanbanTaskResponse toKanbanTaskResponse(
            Task task
    ) {
        var assignee = task.getAssignee();

        return new KanbanTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getPriority(),
                task.getDueDate(),
                task.getPosition(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName()
        );
    }
}

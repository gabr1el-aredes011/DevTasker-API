package br.com.devtasker.api.board.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.dto.CreateBoardRequest;
import br.com.devtasker.api.board.dto.UpdateBoardRequest;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNameAlreadyInUseException;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.project.service.ProjectAccessService;

@Service
public class BoardCommandService {

    private final BoardRepository boardRepository;
    private final ProjectAccessService projectAccessService;
    private final BoardWorkflowProvisioningService provisioningService;
    private final ProjectRepository projectRepository;

    public BoardCommandService(
            BoardRepository boardRepository,
            ProjectAccessService projectAccessService,
            BoardWorkflowProvisioningService provisioningService,
            ProjectRepository projectRepository
    ) {
        this.boardRepository = boardRepository;
        this.projectAccessService = projectAccessService;
        this.provisioningService = provisioningService;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public BoardSummaryResponse create(
            Long projectId,
            Long userId,
            CreateBoardRequest request
    ) {
        projectAccessService.requireManagementAccess(projectId, userId);

        Project project = projectRepository
                .findActiveByIdForUpdate(projectId)
                .orElseThrow(ProjectNotFoundException::new);

        String name = normalizeName(request.name());
        requireAvailableName(projectId, name);

        try {
            boolean requiresDefault = boardRepository
                    .findByProject_IdAndDefaultBoardTrueAndArchivedAtIsNull(
                            projectId
                    )
                    .isEmpty();

            return toSummary(
                    requiresDefault
                            ? provisioningService.createDefaultBoard(project, name)
                            : provisioningService.createBoard(project, name)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new BoardNameAlreadyInUseException();
        }
    }

    @Transactional
    public BoardSummaryResponse update(
            Long boardId,
            Long userId,
            UpdateBoardRequest request
    ) {
        Board board = findActiveForUpdate(boardId);
        requireBoardManagement(board, userId);

        String name = normalizeName(request.name());

        if (!board.getName().equalsIgnoreCase(name)) {
            requireAvailableName(board.getProject().getId(), name);
        }

        board.updateName(name);

        try {
            return toSummary(boardRepository.saveAndFlush(board));
        } catch (DataIntegrityViolationException exception) {
            throw new BoardNameAlreadyInUseException();
        }
    }

    @Transactional
    public void archive(
            Long boardId,
            Long userId
    ) {
        Board board = findActive(boardId);
        Long projectId = board.getProject().getId();

        requireBoardManagement(board, userId);
        lockProjectForBoard(projectId);

        board = findActiveForUpdate(boardId);
        boolean wasDefault = board.isDefaultBoard();

        board.archive();
        boardRepository.saveAndFlush(board);

        if (wasDefault) {
            boardRepository
                    .findFirstByProject_IdAndArchivedAtIsNullOrderByIdAsc(
                            projectId
                    )
                    .ifPresent(this::markAsDefault);
        }
    }

    @Transactional
    public BoardSummaryResponse setDefault(
            Long boardId,
            Long userId
    ) {
        Board requestedBoard = findActive(boardId);
        Long projectId = requestedBoard.getProject().getId();

        requireBoardManagement(requestedBoard, userId);
        lockProjectForBoard(projectId);

        Board board = findActiveForUpdate(boardId);

        if (board.isDefaultBoard()) {
            return toSummary(board);
        }

        boardRepository
                .findByProject_IdAndDefaultBoardTrueAndArchivedAtIsNull(
                        projectId
                )
                .ifPresent(this::clearDefault);

        board.markAsDefault();

        return toSummary(boardRepository.saveAndFlush(board));
    }

    private Board findActive(Long boardId) {
        return boardRepository.findByIdAndArchivedAtIsNull(boardId)
                .orElseThrow(BoardNotFoundException::new);
    }

    private Board findActiveForUpdate(Long boardId) {
        return boardRepository.findActiveByIdForUpdate(boardId)
                .orElseThrow(BoardNotFoundException::new);
    }

    private void lockProjectForBoard(Long projectId) {
        projectRepository.findActiveByIdForUpdate(projectId)
                .orElseThrow(BoardNotFoundException::new);
    }

    private void clearDefault(Board board) {
        board.clearDefault();
        boardRepository.saveAndFlush(board);
    }

    private void markAsDefault(Board board) {
        board.markAsDefault();
        boardRepository.saveAndFlush(board);
    }

    private void requireBoardManagement(
            Board board,
            Long userId
    ) {
        try {
            projectAccessService.requireManagementAccess(
                    board.getProject().getId(),
                    userId
            );
        } catch (ProjectNotFoundException exception) {
            throw new BoardNotFoundException();
        }
    }

    private void requireAvailableName(
            Long projectId,
            String name
    ) {
        if (boardRepository
                .existsByProject_IdAndArchivedAtIsNullAndNameIgnoreCase(
                        projectId,
                        name
                )) {
            throw new BoardNameAlreadyInUseException();
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private BoardSummaryResponse toSummary(Board board) {
        return new BoardSummaryResponse(
                board.getId(),
                board.getProject().getId(),
                board.getName(),
                board.isDefaultBoard()
        );
    }
}

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
import br.com.devtasker.api.project.service.ProjectAccessService;

@Service
public class BoardCommandService {

    private final BoardRepository boardRepository;
    private final ProjectAccessService projectAccessService;
    private final BoardWorkflowProvisioningService provisioningService;

    public BoardCommandService(
            BoardRepository boardRepository,
            ProjectAccessService projectAccessService,
            BoardWorkflowProvisioningService provisioningService
    ) {
        this.boardRepository = boardRepository;
        this.projectAccessService = projectAccessService;
        this.provisioningService = provisioningService;
    }

    @Transactional
    public BoardSummaryResponse create(
            Long projectId,
            Long userId,
            CreateBoardRequest request
    ) {
        Project project = projectAccessService
                .requireManagementAccess(projectId, userId)
                .getProject();

        String name = normalizeName(request.name());
        requireAvailableName(projectId, name);

        try {
            return toSummary(
                    provisioningService.createBoard(project, name)
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
        Board board = findActiveForUpdate(boardId);
        requireBoardManagement(board, userId);

        board.archive();
        boardRepository.saveAndFlush(board);
    }

    private Board findActiveForUpdate(Long boardId) {
        return boardRepository.findActiveByIdForUpdate(boardId)
                .orElseThrow(BoardNotFoundException::new);
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
                board.getName()
        );
    }
}

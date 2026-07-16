package br.com.devtasker.api.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.dto.BoardColumnResponse;
import br.com.devtasker.api.board.dto.BoardDetailsResponse;
import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class BoardQueryService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectMemberRepository
            projectMemberRepository;

    public BoardQueryService(
            BoardRepository boardRepository,
            BoardColumnRepository boardColumnRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.boardRepository = boardRepository;
        this.boardColumnRepository =
                boardColumnRepository;
        this.projectMemberRepository =
                projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<BoardSummaryResponse> findBoardsByProject(
            Long projectId,
            Long userId
    ) {
        validateProjectMembership(projectId, userId);

        return boardRepository
                .findAllByProject_IdOrderByIdAsc(projectId)
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
                .findById(boardId)
                .orElseThrow(BoardNotFoundException::new);

        Long projectId = board
                .getProject()
                .getId();

        if (!projectMemberRepository
                .existsByProject_IdAndUser_Id(
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
                .existsByProject_IdAndUser_Id(
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
}
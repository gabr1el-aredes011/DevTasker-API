package br.com.devtasker.api.board.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.domain.BoardColumnCategory;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.project.domain.Project;

@Service
public class BoardWorkflowProvisioningService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;

    public BoardWorkflowProvisioningService(
            BoardRepository boardRepository,
            BoardColumnRepository boardColumnRepository
    ) {
        this.boardRepository = boardRepository;
        this.boardColumnRepository = boardColumnRepository;
    }

    public Board createBoard(
            Project project,
            String name
    ) {
        return createBoard(
                project,
                name,
                false
        );
    }

    public Board createDefaultBoard(
            Project project,
            String name
    ) {
        return createBoard(
                project,
                name,
                true
        );
    }

    private Board createBoard(
            Project project,
            String name,
            boolean defaultBoard
    ) {
        Board newBoard = Board.create(project, name);

        if (defaultBoard) {
            newBoard.markAsDefault();
        }

        Board board = boardRepository.saveAndFlush(
                newBoard
        );

        boardColumnRepository.saveAll(
                createStandardWorkflow(board)
        );

        return board;
    }

    private List<BoardColumn> createStandardWorkflow(
            Board board
    ) {
        return List.of(
                BoardColumn.create(
                        board,
                        "Backlog",
                        BoardColumnCategory.BACKLOG,
                        0
                ),
                BoardColumn.create(
                        board,
                        "A Fazer",
                        BoardColumnCategory.TODO,
                        1
                ),
                BoardColumn.create(
                        board,
                        "Em Desenvolvimento",
                        BoardColumnCategory.DOING,
                        2
                ),
                BoardColumn.create(
                        board,
                        "Em Revisão",
                        BoardColumnCategory.REVIEW,
                        3
                ),
                BoardColumn.create(
                        board,
                        "Concluído",
                        BoardColumnCategory.DONE,
                        4
                )
        );
    }
}

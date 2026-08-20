package br.com.devtasker.api.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.domain.BoardColumnCategory;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class ProjectProvisioningService {

    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;

    public ProjectProvisioningService(
            ProjectMemberRepository projectMemberRepository,
            BoardRepository boardRepository,
            BoardColumnRepository boardColumnRepository
    ) {
        this.projectMemberRepository =
                projectMemberRepository;

        this.boardRepository =
                boardRepository;

        this.boardColumnRepository =
                boardColumnRepository;
    }

    @Transactional
    public void provisionDefaultStructure(
            Project project,
            UserAccount owner
    ) {
        projectMemberRepository.save(
                ProjectMember.createOwner(
                        project,
                        owner
                )
        );

        Board board = boardRepository.save(
                Board.createInitial(project)
        );

        List<BoardColumn> columns =
                List.of(
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

        boardColumnRepository.saveAll(
                columns
        );
    }
}
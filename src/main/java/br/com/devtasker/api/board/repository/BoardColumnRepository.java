package br.com.devtasker.api.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.board.domain.BoardColumn;

public interface BoardColumnRepository
        extends JpaRepository<BoardColumn, Long> {

    List<BoardColumn> findAllByBoard_IdOrderByPositionAsc(
            Long boardId
    );
}
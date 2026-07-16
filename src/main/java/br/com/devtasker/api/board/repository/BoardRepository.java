package br.com.devtasker.api.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.board.domain.Board;

public interface BoardRepository
        extends JpaRepository<Board, Long> {

    List<Board> findAllByProject_IdOrderByIdAsc(
            Long projectId
    );
}
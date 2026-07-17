package br.com.devtasker.api.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.board.domain.Board;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BoardRepository
        extends JpaRepository<Board, Long> {

    List<Board> findAllByProject_IdOrderByIdAsc(
            Long projectId
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT board
            FROM Board board
            WHERE board.id = :boardId
            """)
    Optional<Board> findByIdForUpdate(
            @Param("boardId") Long boardId
    );
}
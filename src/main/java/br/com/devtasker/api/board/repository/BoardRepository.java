package br.com.devtasker.api.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.board.domain.Board;
import jakarta.persistence.LockModeType;

public interface BoardRepository
        extends JpaRepository<Board, Long> {

    List<Board> findAllByProject_IdAndArchivedAtIsNullOrderByIdAsc(
            Long projectId
    );

    Optional<Board> findByIdAndArchivedAtIsNull(
            Long boardId
    );

    boolean existsByProject_IdAndArchivedAtIsNullAndNameIgnoreCase(
            Long projectId,
            String name
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT board
            FROM Board board
            WHERE board.id = :boardId
              AND board.archivedAt IS NULL
            """)
    Optional<Board> findActiveByIdForUpdate(
            @Param("boardId") Long boardId
    );
}

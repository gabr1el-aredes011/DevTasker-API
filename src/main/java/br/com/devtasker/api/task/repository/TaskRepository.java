package br.com.devtasker.api.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.task.domain.Task;

public interface TaskRepository
        extends JpaRepository<Task, Long> {

    List<Task>
    findAllByColumn_IdAndArchivedAtIsNullOrderByPositionAsc(
            Long columnId
    );

    @Query("""
            SELECT COALESCE(MAX(task.position), -1)
            FROM Task task
            WHERE task.column.id = :columnId
              AND task.archivedAt IS NULL
            """)
    Integer findMaximumActivePositionByColumnId(
            @Param("columnId") Long columnId
    );
    
    @Query("""
            SELECT task
            FROM Task task
            WHERE task.id = :taskId
              AND task.archivedAt IS NULL
              AND task.column.board.archivedAt IS NULL
            """)
    Optional<Task> findActiveById(
            @Param("taskId") Long taskId
    );
    
    @Query("""
            SELECT task.column.board.id
            FROM Task task
            WHERE task.id = :taskId
              AND task.archivedAt IS NULL
              AND task.column.board.archivedAt IS NULL
            """)
    Optional<Long> findBoardIdByActiveTaskId(
            @Param("taskId") Long taskId
    );
    
    @Query("""
            SELECT task
            FROM Task task
            JOIN FETCH task.column boardColumn
            JOIN FETCH boardColumn.board board
            LEFT JOIN FETCH task.assignee assignee
            WHERE board.id = :boardId
              AND board.archivedAt IS NULL
              AND board.project.archivedAt IS NULL
              AND task.archivedAt IS NULL
            ORDER BY boardColumn.position ASC, task.position ASC
            """)
    List<Task> findAllActiveByBoardId(
            @Param("boardId") Long boardId
    );
}

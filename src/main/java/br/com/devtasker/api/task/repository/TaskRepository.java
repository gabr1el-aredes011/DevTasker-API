package br.com.devtasker.api.task.repository;

import java.util.List;

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
}
package br.com.devtasker.api.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.project.domain.Project;
import jakarta.persistence.LockModeType;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT project
            FROM Project project
            WHERE project.id = :projectId
              AND project.archivedAt IS NULL
            """)
    Optional<Project> findActiveByIdForUpdate(
            @Param("projectId") Long projectId
    );
}

package br.com.devtasker.api.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.project.domain.ProjectMember;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN FETCH membership.project project
            WHERE membership.user.id = :userId
              AND project.archivedAt IS NULL
              AND (
                    :query IS NULL
                    OR LOWER(project.name) LIKE LOWER(
                        CONCAT('%', :query, '%')
                    )
                    OR LOWER(COALESCE(project.description, '')) LIKE LOWER(
                        CONCAT('%', :query, '%')
                    )
              )
            ORDER BY project.updatedAt DESC, project.id DESC
            """)
    List<ProjectMember> findActiveProjectsByUser(
            @Param("userId") Long userId,
            @Param("query") String query
    );

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN FETCH membership.project project
            JOIN FETCH project.owner
            WHERE project.id = :projectId
              AND membership.user.id = :userId
              AND project.archivedAt IS NULL
            """)
    Optional<ProjectMember> findActiveMembership(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT CASE WHEN COUNT(membership) > 0 THEN true ELSE false END
            FROM ProjectMember membership
            WHERE membership.project.id = :projectId
              AND membership.user.id = :userId
              AND membership.project.archivedAt IS NULL
            """)
    boolean existsActiveMembership(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );
}

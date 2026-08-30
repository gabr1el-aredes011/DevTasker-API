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
            JOIN FETCH membership.user
            WHERE membership.project.id = :projectId
              AND membership.project.archivedAt IS NULL
            """)
    List<ProjectMember> findActiveMembersByProject(
            @Param("projectId") Long projectId
    );

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN FETCH membership.project project
            WHERE membership.user.id = :userId
              AND project.archivedAt IS NULL
            ORDER BY project.updatedAt DESC, project.id DESC
            """)
    List<ProjectMember> findActiveProjectsByUser(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN FETCH membership.project project
            WHERE membership.user.id = :userId
              AND project.archivedAt IS NULL
              AND (
                    LOWER(project.name) LIKE CONCAT(
                        '%', LOWER(:query), '%'
                    )
                    OR LOWER(COALESCE(project.description, '')) LIKE CONCAT(
                        '%', LOWER(:query), '%'
                    )
              )
            ORDER BY project.updatedAt DESC, project.id DESC
            """)
    List<ProjectMember> searchActiveProjectsByUser(
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

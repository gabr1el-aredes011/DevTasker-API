package br.com.devtasker.api.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.project.domain.ProjectInvitation;
import br.com.devtasker.api.project.domain.ProjectInvitationStatus;
import jakarta.persistence.LockModeType;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    @Query("""
            SELECT invitation
            FROM ProjectInvitation invitation
            JOIN FETCH invitation.project project
            JOIN FETCH invitation.invitedBy
            WHERE project.id = :projectId
              AND project.archivedAt IS NULL
              AND invitation.status = :status
            ORDER BY invitation.createdAt DESC, invitation.id DESC
            """)
    List<ProjectInvitation> findByProjectAndStatus(
            @Param("projectId") Long projectId,
            @Param("status") ProjectInvitationStatus status
    );

    @Query("""
            SELECT invitation
            FROM ProjectInvitation invitation
            WHERE invitation.project.id = :projectId
              AND invitation.invitedEmail = :email
              AND invitation.status = :status
            """)
    Optional<ProjectInvitation> findByProjectEmailAndStatus(
            @Param("projectId") Long projectId,
            @Param("email") String email,
            @Param("status") ProjectInvitationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT invitation
            FROM ProjectInvitation invitation
            JOIN FETCH invitation.project project
            JOIN FETCH invitation.invitedBy
            WHERE invitation.tokenHash = :tokenHash
            """)
    Optional<ProjectInvitation> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT invitation
            FROM ProjectInvitation invitation
            JOIN FETCH invitation.project project
            WHERE invitation.id = :invitationId
              AND project.id = :projectId
              AND project.archivedAt IS NULL
            """)
    Optional<ProjectInvitation> findByIdForUpdate(
            @Param("projectId") Long projectId,
            @Param("invitationId") Long invitationId
    );
}

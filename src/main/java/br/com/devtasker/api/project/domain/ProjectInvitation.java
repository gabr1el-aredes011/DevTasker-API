package br.com.devtasker.api.project.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private UserAccount invitedBy;

    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectMemberRole role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    private ProjectInvitation(
            Project project,
            UserAccount invitedBy,
            String invitedEmail,
            ProjectMemberRole role,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {
        if (role == ProjectMemberRole.OWNER) {
            throw new IllegalArgumentException("Convites não podem conceder a função OWNER.");
        }

        this.project = project;
        this.invitedBy = invitedBy;
        this.invitedEmail = invitedEmail;
        this.role = role;
        this.tokenHash = tokenHash;
        this.status = ProjectInvitationStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public static ProjectInvitation create(
            Project project,
            UserAccount invitedBy,
            String invitedEmail,
            ProjectMemberRole role,
            String tokenHash,
            OffsetDateTime expiresAt
    ) {
        return new ProjectInvitation(
                project,
                invitedBy,
                invitedEmail,
                role,
                tokenHash,
                expiresAt
        );
    }

    public boolean isExpired(OffsetDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void accept(OffsetDateTime now) {
        requirePending();
        this.status = ProjectInvitationStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void revoke(OffsetDateTime now) {
        requirePending();
        this.status = ProjectInvitationStatus.REVOKED;
        this.respondedAt = now;
    }

    public void expire(OffsetDateTime now) {
        if (status == ProjectInvitationStatus.PENDING) {
            this.status = ProjectInvitationStatus.EXPIRED;
            this.respondedAt = now;
        }
    }

    private void requirePending() {
        if (status != ProjectInvitationStatus.PENDING) {
            throw new IllegalStateException("O convite não está pendente.");
        }
    }

    @PrePersist
    private void beforeInsert() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

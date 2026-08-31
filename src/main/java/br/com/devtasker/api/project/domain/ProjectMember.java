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
@Table(name = "project_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    private ProjectMember(
            Project project,
            UserAccount user,
            ProjectMemberRole role
    ) {
        this.project = project;
        this.user = user;
        this.role = role;
    }

    public static ProjectMember createOwner(
            Project project,
            UserAccount user
    ) {
        return new ProjectMember(
                project,
                user,
                ProjectMemberRole.OWNER
        );
    }

    public static ProjectMember create(
            Project project,
            UserAccount user,
            ProjectMemberRole role
    ) {
        if (role == ProjectMemberRole.OWNER) {
            throw new IllegalArgumentException("Novos membros não podem receber a função OWNER.");
        }

        return new ProjectMember(project, user, role);
    }

    public void changeRole(ProjectMemberRole role) {
        if (this.role == ProjectMemberRole.OWNER || role == ProjectMemberRole.OWNER) {
            throw new IllegalArgumentException("A propriedade do projeto não pode ser alterada.");
        }

        this.role = role;
    }

    @PrePersist
    private void beforeInsert() {
        this.joinedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }
}

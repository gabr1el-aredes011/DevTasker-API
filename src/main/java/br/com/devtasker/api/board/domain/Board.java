package br.com.devtasker.api.board.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import br.com.devtasker.api.project.domain.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    private static final int MAX_NAME_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @Column(name = "is_default", nullable = false)
    private boolean defaultBoard;

    private Board(Project project, String name) {
        if (project == null) {
            throw new IllegalArgumentException(
                    "O projeto do quadro é obrigatório."
            );
        }

        this.project = project;
        this.name = normalizeName(name);
    }

    public static Board create(
            Project project,
            String name
    ) {
        return new Board(project, name);
    }

    public static Board createInitial(Project project) {
        Board board = create(
                project,
                "Quadro Principal"
        );

        board.markAsDefault();
        return board;
    }

    public void updateName(String name) {
        requireActive();
        this.name = normalizeName(name);
    }

    public void markAsDefault() {
        requireActive();
        this.defaultBoard = true;
    }

    public void clearDefault() {
        this.defaultBoard = false;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive() {
        requireActive();
        clearDefault();
        this.archivedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void requireActive() {
        if (isArchived()) {
            throw new IllegalStateException(
                    "O quadro já está arquivado."
            );
        }
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do quadro é obrigatório."
            );
        }

        String normalized = name.trim();

        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "O nome do quadro deve possuir no máximo 120 caracteres."
            );
        }

        return normalized;
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        this.updatedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }
}

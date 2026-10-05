package br.com.devtasker.api.project.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_labels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectLabel {

    public static final int MAXIMUM_NAME_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectLabelColor color;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    private ProjectLabel(Project project, String name, ProjectLabelColor color) {
        if (project == null) {
            throw new IllegalArgumentException("O projeto da label é obrigatório.");
        }

        this.project = project;
        update(name, color);
    }

    public static ProjectLabel create(Project project, String name, ProjectLabelColor color) {
        return new ProjectLabel(project, name, color);
    }

    public void update(String name, ProjectLabelColor color) {
        String normalized = normalizeName(name);

        if (color == null) {
            throw new IllegalArgumentException("A cor da label é obrigatória.");
        }

        this.name = normalized;
        this.normalizedName = normalized.toLowerCase(Locale.ROOT);
        this.color = color;
    }

    public void archive() {
        if (archivedAt == null) {
            archivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da label é obrigatório.");
        }

        String normalized = name.trim();
        if (normalized.length() > MAXIMUM_NAME_LENGTH) {
            throw new IllegalArgumentException("O nome da label deve possuir no máximo 30 caracteres.");
        }

        return normalized;
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

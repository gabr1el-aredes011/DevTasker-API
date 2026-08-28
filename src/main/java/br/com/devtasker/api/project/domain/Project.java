package br.com.devtasker.api.project.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import br.com.devtasker.api.user.domain.UserAccount;
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
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    private Project(
            String name,
            String description,
            UserAccount owner
    ) {
        if (owner == null) {
            throw new IllegalArgumentException(
                    "O proprietário do projeto é obrigatório."
            );
        }

        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.owner = owner;
    }

    public static Project create(
            String name,
            String description,
            UserAccount owner
    ) {
        return new Project(
                name,
                description,
                owner
        );
    }

    public static Project createInitial(
            UserAccount owner
    ) {
        return create(
                "Meu Primeiro Projeto",
                "Projeto inicial criado automaticamente pelo DevTasker.",
                owner
        );
    }

    public void updateDetails(
            String name,
            String description
    ) {
        requireActive();

        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive() {
        requireActive();

        this.archivedAt =
                OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void requireActive() {
        if (isArchived()) {
            throw new IllegalStateException(
                    "O projeto já está arquivado."
            );
        }
    }

    private static String normalizeName(
            String name
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do projeto é obrigatório."
            );
        }

        String normalized = name.trim();

        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "O nome do projeto deve possuir no máximo 120 caracteres."
            );
        }

        return normalized;
    }

    private static String normalizeDescription(
            String description
    ) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "A descrição do projeto deve possuir no máximo 1000 caracteres."
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

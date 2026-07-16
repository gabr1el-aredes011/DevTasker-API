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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private Board(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    public static Board createInitial(Project project) {
        return new Board(
                project,
                "Quadro Principal"
        );
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

package br.com.devtasker.api.task.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
@Table(name = "task_checklist_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskChecklistItem {

    public static final int MAXIMUM_TITLE_LENGTH = 180;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = MAXIMUM_TITLE_LENGTH)
    private String title;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private TaskChecklistItem(Task task, String title, Integer position) {
        if (task == null) {
            throw new IllegalArgumentException("A tarefa do item é obrigatória.");
        }

        if (position == null || position < 0) {
            throw new IllegalArgumentException("A posição do item não pode ser negativa.");
        }

        this.task = task;
        this.position = position;
        this.completed = false;
        update(title, false);
    }

    static TaskChecklistItem create(Task task, String title, Integer position) {
        return new TaskChecklistItem(task, title, position);
    }

    public void update(String title, boolean completed) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título do item é obrigatório.");
        }

        String normalizedTitle = title.trim();

        if (normalizedTitle.length() > MAXIMUM_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "O título do item deve possuir no máximo 180 caracteres."
            );
        }

        this.title = normalizedTitle;
        this.completed = completed;
    }

    @PrePersist
    private void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void beforeUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

package br.com.devtasker.api.task.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import br.com.devtasker.api.board.domain.BoardColumn;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserAccount creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserAccount assignee;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    private Task(
            BoardColumn column,
            UserAccount creator,
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Integer position
    ) {
        if (position == null || position < 0) {
            throw new IllegalArgumentException(
                    "A posição da tarefa não pode ser negativa."
            );
        }

        this.column = column;
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.position = position;
    }

    public static Task create(
            BoardColumn column,
            UserAccount creator,
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate,
            Integer position
    ) {
        return new Task(
                column,
                creator,
                title,
                description,
                priority,
                dueDate,
                position
        );
    }
    
    public void updateDetails(
            String title,
            String description,
            TaskPriority priority,
            LocalDate dueDate
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "O título da tarefa é obrigatório."
            );
        }

        if (priority == null) {
            throw new IllegalArgumentException(
                    "A prioridade da tarefa é obrigatória."
            );
        }

        this.title = title.trim();
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public void assignTo(UserAccount assignee) {
        this.assignee = assignee;
    }

    public void archive() {
        if (this.archivedAt == null) {
            this.archivedAt =
                    OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
    
    public void relocate(
            BoardColumn targetColumn,
            Integer targetPosition
    ) {
        if (targetColumn == null) {
            throw new IllegalArgumentException(
                    "A coluna de destino é obrigatória."
            );
        }

        if (targetPosition == null || targetPosition < 0) {
            throw new IllegalArgumentException(
                    "A posição da tarefa não pode ser negativa."
            );
        }

        this.column = targetColumn;
        this.position = targetPosition;
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

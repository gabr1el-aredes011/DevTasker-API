package br.com.devtasker.api.task.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    private static final int MAXIMUM_LABELS = 5;
    private static final int MAXIMUM_LABEL_LENGTH = 30;

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

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "task_labels",
            joinColumns = @JoinColumn(name = "task_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "label", nullable = false, length = MAXIMUM_LABEL_LENGTH)
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    private List<String> labels = new ArrayList<>();

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

    public List<String> getLabels() {
        return List.copyOf(labels);
    }

    public void replaceLabels(List<String> requestedLabels) {
        Map<String, String> normalizedLabels = new LinkedHashMap<>();

        if (requestedLabels != null) {
            for (String requestedLabel : requestedLabels) {
                if (requestedLabel == null || requestedLabel.isBlank()) {
                    throw new IllegalArgumentException(
                            "As labels da tarefa não podem estar vazias."
                    );
                }

                String normalizedLabel = requestedLabel.trim();

                if (normalizedLabel.length() > MAXIMUM_LABEL_LENGTH) {
                    throw new IllegalArgumentException(
                            "Cada label deve possuir no máximo 30 caracteres."
                    );
                }

                normalizedLabels.putIfAbsent(
                        normalizedLabel.toLowerCase(Locale.ROOT),
                        normalizedLabel
                );
            }
        }

        if (normalizedLabels.size() > MAXIMUM_LABELS) {
            throw new IllegalArgumentException(
                    "Uma tarefa pode possuir no máximo 5 labels."
            );
        }

        labels.clear();
        labels.addAll(normalizedLabels.values());
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

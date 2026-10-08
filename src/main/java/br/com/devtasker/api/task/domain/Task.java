package br.com.devtasker.api.task.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

    public static final int MAXIMUM_DESCRIPTION_LENGTH = 4000;
    public static final int MAXIMUM_LABELS = 5;
    public static final int MAXIMUM_TECHNOLOGIES = 8;

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

    @Column(length = MAXIMUM_DESCRIPTION_LENGTH)
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_label_assignments",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @OrderColumn(name = "position")
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    private List<ProjectLabel> labels = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "task_technologies",
            joinColumns = @JoinColumn(name = "task_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "technology", nullable = false, length = 32)
    @OrderColumn(name = "position")
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    private List<TaskTechnology> technologies = new ArrayList<>();

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    private List<TaskChecklistItem> checklistItems = new ArrayList<>();

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

        validateDescription(description);

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

        validateDescription(description);

        this.title = title.trim();
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > MAXIMUM_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "A descrição deve possuir no máximo 4000 caracteres."
            );
        }
    }

    public void assignTo(UserAccount assignee) {
        this.assignee = assignee;
    }

    public List<ProjectLabel> getLabels() {
        return List.copyOf(labels);
    }

    public void replaceLabels(List<ProjectLabel> requestedLabels) {
        LinkedHashSet<ProjectLabel> uniqueLabels = new LinkedHashSet<>(
                requestedLabels == null ? List.of() : requestedLabels
        );

        if (uniqueLabels.contains(null)) {
            throw new IllegalArgumentException("As labels da tarefa são inválidas.");
        }

        if (uniqueLabels.size() > MAXIMUM_LABELS) {
            throw new IllegalArgumentException(
                    "Uma tarefa pode possuir no máximo 5 labels."
            );
        }

        labels.clear();
        labels.addAll(uniqueLabels);
    }

    public List<TaskTechnology> getTechnologies() {
        return List.copyOf(technologies);
    }

    public void replaceTechnologies(List<TaskTechnology> requestedTechnologies) {
        LinkedHashSet<TaskTechnology> uniqueTechnologies = new LinkedHashSet<>(
                requestedTechnologies == null ? List.of() : requestedTechnologies
        );

        if (uniqueTechnologies.contains(null)) {
            throw new IllegalArgumentException("As tecnologias da tarefa são inválidas.");
        }

        if (uniqueTechnologies.size() > MAXIMUM_TECHNOLOGIES) {
            throw new IllegalArgumentException(
                    "Uma tarefa pode possuir no máximo 8 tecnologias."
            );
        }

        technologies.clear();
        technologies.addAll(uniqueTechnologies);
    }

    public List<TaskChecklistItem> getChecklistItems() {
        return List.copyOf(checklistItems);
    }

    public TaskChecklistItem addChecklistItem(String title) {
        int nextPosition = checklistItems.stream()
                .mapToInt(TaskChecklistItem::getPosition)
                .max()
                .orElse(-1) + 1;

        TaskChecklistItem item = TaskChecklistItem.create(
                this,
                title,
                nextPosition
        );

        checklistItems.add(item);
        return item;
    }

    public Optional<TaskChecklistItem> findChecklistItem(Long itemId) {
        return checklistItems.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst();
    }

    public void removeChecklistItem(TaskChecklistItem item) {
        checklistItems.remove(item);
    }

    public void recordActivity() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
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

package br.com.devtasker.api.task.domain;

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
@Table(name = "task_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserAccount actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TaskActivityType type;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private TaskActivity(
            Task task,
            UserAccount actor,
            TaskActivityType type,
            String description
    ) {
        if (task == null || actor == null || type == null) {
            throw new IllegalArgumentException("A atividade deve possuir tarefa, autor e tipo.");
        }

        if (description == null || description.isBlank() || description.length() > 500) {
            throw new IllegalArgumentException("A descrição da atividade é inválida.");
        }

        this.task = task;
        this.actor = actor;
        this.type = type;
        this.description = description.trim();
    }

    public static TaskActivity create(
            Task task,
            UserAccount actor,
            TaskActivityType type,
            String description
    ) {
        return new TaskActivity(task, actor, type, description);
    }

    @PrePersist
    private void beforeInsert() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

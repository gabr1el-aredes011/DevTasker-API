package br.com.devtasker.api.task.domain;

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
@Table(name = "task_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskComment {

    public static final int MAXIMUM_CONTENT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private TaskComment parentComment;

    @Column(nullable = false, length = MAXIMUM_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    private TaskComment(
            Task task,
            UserAccount author,
            TaskComment parentComment,
            String content
    ) {
        if (task == null || author == null) {
            throw new IllegalArgumentException("O comentário deve possuir tarefa e autor.");
        }

        if (parentComment != null && parentComment.getTask() != task) {
            throw new IllegalArgumentException("A resposta deve pertencer à mesma tarefa.");
        }

        this.task = task;
        this.author = author;
        this.parentComment = parentComment == null ? null : parentComment.root();
        changeContent(content);
    }

    public static TaskComment create(Task task, UserAccount author, String content) {
        return new TaskComment(task, author, null, content);
    }

    public static TaskComment replyTo(
            Task task,
            UserAccount author,
            TaskComment parentComment,
            String content
    ) {
        if (parentComment == null) {
            throw new IllegalArgumentException("A resposta deve possuir um comentário de origem.");
        }

        return new TaskComment(task, author, parentComment, content);
    }

    public TaskComment root() {
        return parentComment == null ? this : parentComment;
    }

    public void edit(String content) {
        changeContent(content);
        this.editedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void remove() {
        if (deletedAt == null) {
            deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private void changeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("O comentário não pode estar vazio.");
        }

        String normalized = content.trim();
        if (normalized.length() > MAXIMUM_CONTENT_LENGTH) {
            throw new IllegalArgumentException("O comentário deve possuir no máximo 2000 caracteres.");
        }

        this.content = normalized;
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

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private UserAccount uploader;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, unique = true, length = 80)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    private TaskAttachment(
            Task task,
            UserAccount uploader,
            String originalFileName,
            String storageKey,
            String contentType,
            Long sizeBytes
    ) {
        if (task == null || uploader == null) {
            throw new IllegalArgumentException("O anexo deve possuir tarefa e autor.");
        }

        if (originalFileName == null || originalFileName.isBlank()
                || originalFileName.length() > 255) {
            throw new IllegalArgumentException("O nome do anexo é inválido.");
        }

        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("A chave de armazenamento é obrigatória.");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("O tipo do anexo é obrigatório.");
        }

        if (sizeBytes == null || sizeBytes <= 0) {
            throw new IllegalArgumentException("O tamanho do anexo é inválido.");
        }

        this.task = task;
        this.uploader = uploader;
        this.originalFileName = originalFileName.trim();
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public static TaskAttachment create(
            Task task,
            UserAccount uploader,
            String originalFileName,
            String storageKey,
            String contentType,
            Long sizeBytes
    ) {
        return new TaskAttachment(
                task,
                uploader,
                originalFileName,
                storageKey,
                contentType,
                sizeBytes
        );
    }

    public void remove() {
        if (deletedAt == null) {
            deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    @PrePersist
    private void beforeInsert() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

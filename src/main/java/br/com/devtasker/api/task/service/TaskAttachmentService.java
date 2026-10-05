package br.com.devtasker.api.task.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.exception.TaskAttachmentException;
import br.com.devtasker.api.exception.TaskAttachmentNotFoundException;
import br.com.devtasker.api.exception.TaskNotFoundException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.service.ProjectAccessService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.domain.TaskAttachment;
import br.com.devtasker.api.task.dto.TaskAttachmentDownload;
import br.com.devtasker.api.task.dto.TaskAttachmentResponse;
import br.com.devtasker.api.task.dto.TaskUserSummaryResponse;
import br.com.devtasker.api.task.repository.TaskAttachmentRepository;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.task.storage.TaskAttachmentStorage;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class TaskAttachmentService {

    static final int MAX_ATTACHMENTS_PER_TASK = 10;
    static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp",
            "text/plain",
            "application/json",
            "application/zip",
            "application/x-zip-compressed"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAttachmentService.class);

    private final TaskRepository taskRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final ProjectAccessService projectAccessService;
    private final TaskActivityRecorder activityRecorder;
    private final TaskAttachmentStorage storage;

    public TaskAttachmentService(
            TaskRepository taskRepository,
            TaskAttachmentRepository attachmentRepository,
            ProjectAccessService projectAccessService,
            TaskActivityRecorder activityRecorder,
            TaskAttachmentStorage storage
    ) {
        this.taskRepository = taskRepository;
        this.attachmentRepository = attachmentRepository;
        this.projectAccessService = projectAccessService;
        this.activityRecorder = activityRecorder;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> findAll(Long taskId, Long userId) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireMembership(projectId(task), userId);
        return toResponseList(task, membership);
    }

    @Transactional
    public List<TaskAttachmentResponse> upload(
            Long taskId,
            Long userId,
            MultipartFile file
    ) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireWriteAccess(projectId(task), userId);

        if (attachmentRepository.countByTask_IdAndDeletedAtIsNull(taskId)
                >= MAX_ATTACHMENTS_PER_TASK) {
            throw TaskAttachmentException.limitExceeded();
        }

        String fileName = validateFileName(file);
        String contentType = validateContentType(file);
        validateFileSize(file);

        String storageKey = UUID.randomUUID().toString();
        storeFile(storageKey, file);
        registerRollbackCleanup(storageKey);

        TaskAttachment attachment = TaskAttachment.create(
                task,
                membership.getUser(),
                fileName,
                storageKey,
                contentType,
                file.getSize()
        );
        attachmentRepository.saveAndFlush(attachment);
        activityRecorder.record(
                task,
                membership.getUser(),
                TaskActivityType.ATTACHMENT_ADDED,
                "anexou o arquivo \"" + fileName + "\"."
        );
        task.recordActivity();
        taskRepository.save(task);

        return toResponseList(task, membership);
    }

    @Transactional(readOnly = true)
    public TaskAttachmentDownload download(Long taskId, Long attachmentId, Long userId) {
        Task task = findActiveTask(taskId);
        projectAccessService.requireMembership(projectId(task), userId);
        TaskAttachment attachment = findAttachment(taskId, attachmentId);

        try {
            return new TaskAttachmentDownload(
                    storage.load(attachment.getStorageKey()),
                    attachment.getOriginalFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes()
            );
        } catch (IOException exception) {
            LOGGER.error("Falha ao carregar o anexo {}.", attachment.getId(), exception);
            throw TaskAttachmentException.storageFailure();
        }
    }

    @Transactional
    public List<TaskAttachmentResponse> remove(
            Long taskId,
            Long attachmentId,
            Long userId
    ) {
        Task task = findActiveTask(taskId);
        ProjectMember membership = projectAccessService.requireWriteAccess(projectId(task), userId);
        TaskAttachment attachment = findAttachment(taskId, attachmentId);

        boolean ownsAttachment = attachment.getUploader().getId().equals(userId);
        boolean canModerate = canModerate(membership);
        if (!ownsAttachment && !canModerate) {
            throw new ProjectPermissionDeniedException();
        }

        attachment.remove();
        activityRecorder.record(
                task,
                membership.getUser(),
                TaskActivityType.ATTACHMENT_REMOVED,
                "removeu o anexo \"" + attachment.getOriginalFileName() + "\"."
        );
        task.recordActivity();
        taskRepository.save(task);
        registerCommitDeletion(attachment.getStorageKey());

        return toResponseList(task, membership);
    }

    private List<TaskAttachmentResponse> toResponseList(
            Task task,
            ProjectMember membership
    ) {
        return attachmentRepository
                .findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(task.getId())
                .stream()
                .map(attachment -> toResponse(attachment, membership))
                .toList();
    }

    private TaskAttachmentResponse toResponse(
            TaskAttachment attachment,
            ProjectMember membership
    ) {
        boolean canDelete = membership.getRole() != ProjectMemberRole.VIEWER
                && (canModerate(membership)
                || attachment.getUploader().getId().equals(membership.getUser().getId()));

        return new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                toUserResponse(attachment.getUploader()),
                canDelete,
                attachment.getCreatedAt()
        );
    }

    private TaskUserSummaryResponse toUserResponse(UserAccount user) {
        return new TaskUserSummaryResponse(user.getId(), user.getName(), user.getProfileImageUrl());
    }

    private String validateFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw TaskAttachmentException.invalid("Selecione um arquivo não vazio.");
        }

        String originalFileName = file.getOriginalFilename();
        String cleanedFileName = StringUtils.cleanPath(
                originalFileName == null ? "" : originalFileName
        ).trim();

        if (cleanedFileName.isBlank()
                || cleanedFileName.length() > 255
                || cleanedFileName.contains("..")
                || cleanedFileName.contains("/")
                || cleanedFileName.contains("\\")
                || cleanedFileName.chars().anyMatch(Character::isISOControl)) {
            throw TaskAttachmentException.invalid("O nome do arquivo é inválido.");
        }

        return cleanedFileName;
    }

    private String validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw TaskAttachmentException.invalid(
                    "Tipo de arquivo não permitido. Use PDF, PNG, JPEG, WebP, TXT, JSON ou ZIP."
            );
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw TaskAttachmentException.tooLarge();
        }
    }

    private void storeFile(String storageKey, MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            storage.store(storageKey, content);
        } catch (IOException exception) {
            LOGGER.error("Falha ao armazenar um novo anexo.", exception);
            throw TaskAttachmentException.storageFailure();
        }
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteQuietly(storageKey);
                }
            }
        });
    }

    private void registerCommitDeletion(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(storageKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storageKey);
            }
        });
    }

    private void deleteQuietly(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (IOException exception) {
            LOGGER.warn("Não foi possível remover o arquivo físico {}.", storageKey, exception);
        }
    }

    private Task findActiveTask(Long taskId) {
        return taskRepository.findActiveById(taskId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private TaskAttachment findAttachment(Long taskId, Long attachmentId) {
        return attachmentRepository
                .findByIdAndTask_IdAndDeletedAtIsNull(attachmentId, taskId)
                .orElseThrow(TaskAttachmentNotFoundException::new);
    }

    private boolean canModerate(ProjectMember membership) {
        return membership.getRole() == ProjectMemberRole.OWNER
                || membership.getRole() == ProjectMemberRole.ADMIN;
    }

    private Long projectId(Task task) {
        return task.getColumn().getBoard().getProject().getId();
    }
}

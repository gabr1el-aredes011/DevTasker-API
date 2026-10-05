package br.com.devtasker.api.task.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.devtasker.api.task.dto.TaskAttachmentDownload;
import br.com.devtasker.api.task.dto.TaskAttachmentResponse;
import br.com.devtasker.api.task.service.TaskAttachmentService;

@RestController
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    public TaskAttachmentController(TaskAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    public List<TaskAttachmentResponse> findAll(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return attachmentService.findAll(taskId, extractUserId(jwt));
    }

    @PostMapping(
            value = "/api/tasks/{taskId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<TaskAttachmentResponse> upload(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {
        return attachmentService.upload(taskId, extractUserId(jwt), file);
    }

    @GetMapping("/api/tasks/{taskId}/attachments/{attachmentId}/download")
    public ResponseEntity<?> download(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        TaskAttachmentDownload download = attachmentService.download(
                taskId,
                attachmentId,
                extractUserId(jwt)
        );
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/api/tasks/{taskId}/attachments/{attachmentId}")
    public List<TaskAttachmentResponse> remove(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return attachmentService.remove(taskId, attachmentId, extractUserId(jwt));
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        return userId.longValue();
    }
}

package br.com.devtasker.api.task.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.task.dto.TaskCollaborationResponse;
import br.com.devtasker.api.task.dto.TaskCommentRequest;
import br.com.devtasker.api.task.service.TaskCollaborationService;
import jakarta.validation.Valid;

@RestController
public class TaskCollaborationController {

    private final TaskCollaborationService collaborationService;

    public TaskCollaborationController(TaskCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping("/api/tasks/{taskId}/collaboration")
    public TaskCollaborationResponse findByTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return collaborationService.findByTask(taskId, extractUserId(jwt));
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public TaskCollaborationResponse addComment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskCommentRequest request
    ) {
        return collaborationService.addComment(taskId, extractUserId(jwt), request);
    }

    @PatchMapping("/api/tasks/{taskId}/comments/{commentId}")
    public TaskCollaborationResponse editComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskCommentRequest request
    ) {
        return collaborationService.editComment(
                taskId,
                commentId,
                extractUserId(jwt),
                request
        );
    }

    @DeleteMapping("/api/tasks/{taskId}/comments/{commentId}")
    public TaskCollaborationResponse removeComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return collaborationService.removeComment(taskId, commentId, extractUserId(jwt));
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        return userId.longValue();
    }
}

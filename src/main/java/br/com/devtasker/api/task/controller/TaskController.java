package br.com.devtasker.api.task.controller;

import java.net.URI;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.devtasker.api.task.dto.CreateTaskRequest;
import br.com.devtasker.api.task.dto.TaskResponse;
import br.com.devtasker.api.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import br.com.devtasker.api.task.dto.UpdateTaskRequest;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/columns/{columnId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long columnId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse response = taskService.create(
                columnId,
                extractUserId(jwt),
                request
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/tasks/{taskId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/api/columns/{columnId}/tasks")
    public List<TaskResponse> findAllByColumn(
            @PathVariable Long columnId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return taskService.findAllByColumn(
                columnId,
                extractUserId(jwt)
        );
    }

    @GetMapping("/api/tasks/{taskId}")
    public TaskResponse findById(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return taskService.findById(
                taskId,
                extractUserId(jwt)
        );
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");

        return userId.longValue();
    }
    
    @PutMapping("/api/tasks/{taskId}")
    public TaskResponse update(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(
                taskId,
                extractUserId(jwt),
                request
        );
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> archive(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        taskService.archive(
                taskId,
                extractUserId(jwt)
        );

        return ResponseEntity.noContent().build();
    }
}
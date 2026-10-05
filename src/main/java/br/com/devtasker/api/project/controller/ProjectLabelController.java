package br.com.devtasker.api.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.project.dto.CreateProjectLabelRequest;
import br.com.devtasker.api.project.dto.ProjectLabelResponse;
import br.com.devtasker.api.project.dto.UpdateProjectLabelRequest;
import br.com.devtasker.api.project.service.ProjectLabelService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects/{projectId}/labels")
public class ProjectLabelController {

    private final ProjectLabelService projectLabelService;

    public ProjectLabelController(ProjectLabelService projectLabelService) {
        this.projectLabelService = projectLabelService;
    }

    @GetMapping
    public List<ProjectLabelResponse> findAll(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectLabelService.findAll(projectId, extractUserId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectLabelResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectLabelRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectLabelService.create(projectId, extractUserId(jwt), request);
    }

    @PutMapping("/{labelId}")
    public ProjectLabelResponse update(
            @PathVariable Long projectId,
            @PathVariable Long labelId,
            @Valid @RequestBody UpdateProjectLabelRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectLabelService.update(projectId, labelId, extractUserId(jwt), request);
    }

    @DeleteMapping("/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable Long projectId,
            @PathVariable Long labelId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projectLabelService.archive(projectId, labelId, extractUserId(jwt));
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        return userId.longValue();
    }
}

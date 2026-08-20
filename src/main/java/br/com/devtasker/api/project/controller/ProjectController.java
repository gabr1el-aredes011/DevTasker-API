package br.com.devtasker.api.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.service.BoardQueryService;
import br.com.devtasker.api.project.dto.CreateProjectRequest;
import br.com.devtasker.api.project.dto.ProjectDetailsResponse;
import br.com.devtasker.api.project.dto.ProjectSummaryResponse;
import br.com.devtasker.api.project.dto.UpdateProjectRequest;
import br.com.devtasker.api.project.service.ProjectCommandService;
import br.com.devtasker.api.project.service.ProjectQueryService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectQueryService
            projectQueryService;

    private final ProjectCommandService
            projectCommandService;

    private final BoardQueryService
            boardQueryService;

    public ProjectController(
            ProjectQueryService projectQueryService,
            ProjectCommandService projectCommandService,
            BoardQueryService boardQueryService
    ) {
        this.projectQueryService =
                projectQueryService;

        this.projectCommandService =
                projectCommandService;

        this.boardQueryService =
                boardQueryService;
    }

    @GetMapping
    public List<ProjectSummaryResponse>
    findMyProjects(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectQueryService
                .findProjectsByUser(
                        extractUserId(jwt)
                );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetailsResponse create(
            @Valid
            @RequestBody
            CreateProjectRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        return projectCommandService.create(
                extractUserId(jwt),
                request
        );
    }

    @GetMapping("/{projectId}")
    public ProjectDetailsResponse findById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectQueryService.findById(
                projectId,
                extractUserId(jwt)
        );
    }

    @PutMapping("/{projectId}")
    public ProjectDetailsResponse update(
            @PathVariable Long projectId,

            @Valid
            @RequestBody
            UpdateProjectRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        return projectCommandService.update(
                projectId,
                extractUserId(jwt),
                request
        );
    }

    @GetMapping("/{projectId}/boards")
    public List<BoardSummaryResponse>
    findProjectBoards(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return boardQueryService
                .findBoardsByProject(
                        projectId,
                        extractUserId(jwt)
                );
    }

    private Long extractUserId(
            Jwt jwt
    ) {
        Number userId =
                jwt.getClaim("user_id");

        return userId.longValue();
    }
}
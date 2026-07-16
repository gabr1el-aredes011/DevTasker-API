package br.com.devtasker.api.project.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.service.BoardQueryService;
import br.com.devtasker.api.project.dto.ProjectSummaryResponse;
import br.com.devtasker.api.project.service.ProjectQueryService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectQueryService projectQueryService;
    private final BoardQueryService boardQueryService;

    public ProjectController(
            ProjectQueryService projectQueryService,
            BoardQueryService boardQueryService
    ) {
        this.projectQueryService = projectQueryService;
        this.boardQueryService = boardQueryService;
    }

    @GetMapping
    public List<ProjectSummaryResponse> findMyProjects(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return projectQueryService.findProjectsByUser(
                extractUserId(jwt)
        );
    }

    @GetMapping("/{projectId}/boards")
    public List<BoardSummaryResponse> findProjectBoards(
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return boardQueryService.findBoardsByProject(
                projectId,
                extractUserId(jwt)
        );
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");

        return userId.longValue();
    }
}
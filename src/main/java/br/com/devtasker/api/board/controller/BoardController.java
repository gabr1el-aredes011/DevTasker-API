package br.com.devtasker.api.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.board.dto.BoardDetailsResponse;
import br.com.devtasker.api.board.dto.BoardSummaryResponse;
import br.com.devtasker.api.board.dto.KanbanBoardResponse;
import br.com.devtasker.api.board.dto.UpdateBoardRequest;
import br.com.devtasker.api.board.service.BoardCommandService;
import br.com.devtasker.api.board.service.BoardQueryService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardQueryService boardQueryService;
    private final BoardCommandService boardCommandService;

    public BoardController(
            BoardQueryService boardQueryService,
            BoardCommandService boardCommandService
    ) {
        this.boardQueryService = boardQueryService;
        this.boardCommandService = boardCommandService;
    }

    @GetMapping("/{boardId}")
    public BoardDetailsResponse findById(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Number userId = jwt.getClaim("user_id");

        return boardQueryService.findBoardById(
                boardId,
                userId.longValue()
        );
    }
    @GetMapping("/{boardId}/kanban")
    public KanbanBoardResponse findKanban(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Number userId = jwt.getClaim("user_id");

        return boardQueryService.findKanbanByBoardId(
                boardId,
                userId.longValue()
        );
    }

    @PutMapping("/{boardId}")
    public BoardSummaryResponse update(
            @PathVariable Long boardId,
            @Valid @RequestBody UpdateBoardRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return boardCommandService.update(
                boardId,
                extractUserId(jwt),
                request
        );
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> archive(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boardCommandService.archive(
                boardId,
                extractUserId(jwt)
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{boardId}/default")
    public BoardSummaryResponse setDefault(
            @PathVariable Long boardId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return boardCommandService.setDefault(
                boardId,
                extractUserId(jwt)
        );
    }

    private Long extractUserId(Jwt jwt) {
        Number userId = jwt.getClaim("user_id");
        return userId.longValue();
    }
}

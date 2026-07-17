package br.com.devtasker.api.board.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;	
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.board.dto.BoardDetailsResponse;
import br.com.devtasker.api.board.service.BoardQueryService;
import br.com.devtasker.api.board.dto.KanbanBoardResponse;


@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardQueryService boardQueryService;

    public BoardController(
            BoardQueryService boardQueryService
    ) {
        this.boardQueryService = boardQueryService;
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
}
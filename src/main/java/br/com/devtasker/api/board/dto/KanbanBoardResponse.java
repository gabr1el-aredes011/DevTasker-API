package br.com.devtasker.api.board.dto;

import java.util.List;

public record KanbanBoardResponse(
        Long id,
        Long projectId,
        String name,
        List<KanbanColumnResponse> columns
) {
}
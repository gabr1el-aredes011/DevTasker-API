package br.com.devtasker.api.board.dto;

import java.util.List;

import br.com.devtasker.api.board.domain.BoardColumnCategory;

public record KanbanColumnResponse(
        Long id,
        String name,
        BoardColumnCategory category,
        Integer position,
        List<KanbanTaskResponse> tasks
) {
}
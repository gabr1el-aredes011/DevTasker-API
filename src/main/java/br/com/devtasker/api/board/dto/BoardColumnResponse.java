package br.com.devtasker.api.board.dto;

import br.com.devtasker.api.board.domain.BoardColumnCategory;

public record BoardColumnResponse(
        Long id,
        String name,
        BoardColumnCategory category,
        Integer position
) {
}
package br.com.devtasker.api.board.dto;

import java.util.List;

public record BoardDetailsResponse(
        Long id,
        Long projectId,
        String name,
        List<BoardColumnResponse> columns
) {
}
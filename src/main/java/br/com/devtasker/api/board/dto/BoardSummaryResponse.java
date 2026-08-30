package br.com.devtasker.api.board.dto;

public record BoardSummaryResponse(
        Long id,
        Long projectId,
        String name,
        boolean defaultBoard
) {
}

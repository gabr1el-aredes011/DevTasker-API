package br.com.devtasker.api.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
        @NotBlank(message = "Informe o nome do quadro.")
        @Size(max = 120, message = "O nome do quadro deve possuir no máximo 120 caracteres.")
        String name
) {
}

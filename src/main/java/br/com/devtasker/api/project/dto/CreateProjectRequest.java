package br.com.devtasker.api.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank(
                message = "O nome do projeto é obrigatório."
        )
        @Size(
                max = 120,
                message = "O nome do projeto deve possuir no máximo 120 caracteres."
        )
        String name,

        @Size(
                max = 1000,
                message = "A descrição do projeto deve possuir no máximo 1000 caracteres."
        )
        String description
) {
}
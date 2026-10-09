package br.com.devtasker.api.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskCommentRequest(
        @NotBlank(message = "O comentário não pode estar vazio.")
        @Size(max = 2000, message = "O comentário deve possuir no máximo 2000 caracteres.")
        String content,
        @Positive(message = "O comentário de origem deve possuir um identificador válido.")
        Long parentCommentId
) {

    public TaskCommentRequest(String content) {
        this(content, null);
    }
}

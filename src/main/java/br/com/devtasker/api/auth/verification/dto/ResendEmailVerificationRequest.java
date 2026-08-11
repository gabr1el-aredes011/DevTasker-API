package br.com.devtasker.api.auth.verification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendEmailVerificationRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(
                max = 255,
                message = "O e-mail deve possuir no máximo 255 caracteres."
        )
        String email

) {
}
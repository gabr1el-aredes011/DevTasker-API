package br.com.devtasker.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve possuir entre 3 e 100 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um endereço de e-mail válido.")
        @Size(max = 255, message = "O e-mail deve possuir no máximo 255 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve possuir entre 8 e 72 caracteres.")
        String password

) {
}

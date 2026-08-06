package br.com.devtasker.api.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app.mail")
@Validated
public record ApplicationMailProperties(

        @NotBlank(
                message =
                        "O endereço do remetente é obrigatório."
        )
        @Email(
                message =
                        "O endereço do remetente deve ser válido."
        )
        String fromAddress,

        @NotBlank(
                message =
                        "O nome do remetente é obrigatório."
        )
        String fromName

) {
}
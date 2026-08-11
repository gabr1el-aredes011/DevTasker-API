package br.com.devtasker.api.auth.verification.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(
        prefix = "app.email-verification"
)
@Validated
public record EmailVerificationProperties(

        @Min(
                value = 4,
                message =
                        "O código deve possuir ao menos 4 dígitos."
        )
        @Max(
                value = 9,
                message =
                        "O código deve possuir no máximo 9 dígitos."
        )
        int codeLength,

        @Min(
                value = 1,
                message =
                        "A validade deve ser de ao menos 1 minuto."
        )
        long expirationMinutes,

        @Min(
                value = 1,
                message =
                        "O limite de tentativas deve ser positivo."
        )
        int maximumAttempts,

        @Min(
                value = 0,
                message =
                        "O intervalo de reenvio não pode ser negativo."
        )
        long resendIntervalSeconds,

        @NotBlank(
                message =
                        "O segredo HMAC da verificação é obrigatório."
        )
        String hmacSecret

) {

    public Duration expirationDuration() {
        return Duration.ofMinutes(
                expirationMinutes
        );
    }

    public Duration resendInterval() {
        return Duration.ofSeconds(
                resendIntervalSeconds
        );
    }
}
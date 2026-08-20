package br.com.devtasker.api.auth.passwordrecovery.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app.password-recovery")
@Validated
public record PasswordRecoveryProperties(

        @Min(value = 6, message = "O código deve possuir 6 dígitos.")
        @Max(value = 6, message = "O código deve possuir 6 dígitos.")
        int codeLength,

        @Min(
                value = 1,
                message = "A validade do código deve ser positiva."
        )
        long expirationMinutes,

        @Min(
                value = 1,
                message = "O limite de tentativas deve ser positivo."
        )
        int maximumAttempts,

        @Min(
                value = 0,
                message = "O intervalo de reenvio não pode ser negativo."
        )
        long resendIntervalSeconds,

        @Min(
                value = 1,
                message = "A validade do token deve ser positiva."
        )
        long resetTokenExpirationMinutes,

        @NotBlank(
                message = "O segredo HMAC da recuperação é obrigatório."
        )
        String hmacSecret

) {

    public Duration codeExpiration() {
        return Duration.ofMinutes(expirationMinutes);
    }

    public Duration resendInterval() {
        return Duration.ofSeconds(resendIntervalSeconds);
    }

    public Duration resetTokenExpiration() {
        return Duration.ofMinutes(resetTokenExpirationMinutes);
    }
}

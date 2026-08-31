package br.com.devtasker.api.project.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "app.project-invitation")
@Validated
public record ProjectInvitationProperties(
        @Positive int expirationHours,
        @NotBlank String frontendBaseUrl
) {
    public ProjectInvitationProperties {
        if (frontendBaseUrl != null) {
            frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        }
    }

    @NotNull
    public Duration expiration() {
        return Duration.ofHours(expirationHours);
    }
}

package br.com.devtasker.api.auth.passwordrecovery.dto;

public record ResendPasswordRecoveryRequest(
        String challengeId
) {
}

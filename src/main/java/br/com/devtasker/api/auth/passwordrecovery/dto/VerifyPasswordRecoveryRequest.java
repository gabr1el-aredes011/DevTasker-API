package br.com.devtasker.api.auth.passwordrecovery.dto;

public record VerifyPasswordRecoveryRequest(
        String challengeId,
        String code
) {
}

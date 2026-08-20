package br.com.devtasker.api.auth.passwordrecovery.dto;

public record ResetPasswordRequest(
        String resetToken,
        String newPassword
) {
}

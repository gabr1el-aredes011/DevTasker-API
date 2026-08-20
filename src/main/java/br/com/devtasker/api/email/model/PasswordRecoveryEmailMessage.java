package br.com.devtasker.api.email.model;

public record PasswordRecoveryEmailMessage(
        String recipientName,
        String recipientEmail,
        String recoveryCode,
        long expirationMinutes
) {

    public PasswordRecoveryEmailMessage {
        recipientName = requireText(
                recipientName,
                "O nome do destinatário é obrigatório."
        );

        recipientEmail = requireText(
                recipientEmail,
                "O e-mail do destinatário é obrigatório."
        );

        recoveryCode = requireText(
                recoveryCode,
                "O código de recuperação é obrigatório."
        );

        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "A validade do código deve ser positiva."
            );
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}

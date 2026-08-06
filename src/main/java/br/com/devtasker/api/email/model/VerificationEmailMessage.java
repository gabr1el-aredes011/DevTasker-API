package br.com.devtasker.api.email.model;

public record VerificationEmailMessage(
        String recipientName,
        String recipientEmail,
        String verificationCode,
        long expirationMinutes
) {

    public VerificationEmailMessage {
        recipientName =
                requireText(
                        recipientName,
                        "O nome do destinatário é obrigatório."
                );

        recipientEmail =
                requireText(
                        recipientEmail,
                        "O e-mail do destinatário é obrigatório."
                );

        verificationCode =
                requireText(
                        verificationCode,
                        "O código de verificação é obrigatório."
                );

        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "A validade do código deve ser positiva."
            );
        }
    }

    private static String requireText(
            String value,
            String message
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            throw new IllegalArgumentException(
                    message
            );
        }

        return value.trim();
    }
}
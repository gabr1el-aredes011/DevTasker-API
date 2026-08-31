package br.com.devtasker.api.email.model;

public record ProjectInvitationEmailMessage(
        String recipientName,
        String recipientEmail,
        String inviterName,
        String projectName,
        String roleLabel,
        String acceptanceUrl,
        int expirationHours
) {
}

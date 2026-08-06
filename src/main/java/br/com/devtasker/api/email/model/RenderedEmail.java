package br.com.devtasker.api.email.model;

public record RenderedEmail(
        String subject,
        String plainText,
        String htmlText
) {

    public RenderedEmail {
        if (
                subject == null ||
                subject.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O assunto do e-mail é obrigatório."
            );
        }

        if (
                plainText == null ||
                plainText.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O conteúdo textual é obrigatório."
            );
        }

        if (
                htmlText == null ||
                htmlText.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O conteúdo HTML é obrigatório."
            );
        }
    }
}
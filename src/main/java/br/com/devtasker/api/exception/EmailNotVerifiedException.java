package br.com.devtasker.api.exception;

public class EmailNotVerifiedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailNotVerifiedException() {
        super(
                "Confirme seu e-mail antes de acessar o DevTasker."
        );
    }
}

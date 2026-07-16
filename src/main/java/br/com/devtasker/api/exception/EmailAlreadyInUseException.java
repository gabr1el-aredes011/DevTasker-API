package br.com.devtasker.api.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyInUseException() {
        super("Já existe uma conta cadastrada com este e-mail.");
    }
}

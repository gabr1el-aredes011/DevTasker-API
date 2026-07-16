package br.com.devtasker.api.exception;

public class BoardColumnNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BoardColumnNotFoundException() {
        super("Coluna não encontrada.");
    }
}
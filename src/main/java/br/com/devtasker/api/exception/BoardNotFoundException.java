package br.com.devtasker.api.exception;

public class BoardNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BoardNotFoundException() {
        super("Quadro não encontrado.");
    }
}
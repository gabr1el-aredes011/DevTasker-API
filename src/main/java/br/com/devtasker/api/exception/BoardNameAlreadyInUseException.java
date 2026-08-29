package br.com.devtasker.api.exception;

public class BoardNameAlreadyInUseException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BoardNameAlreadyInUseException() {
        super("Já existe um quadro ativo com este nome no projeto.");
    }
}

package br.com.devtasker.api.exception;

public class ProjectNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException() {
        super("Projeto não encontrado.");
    }
}
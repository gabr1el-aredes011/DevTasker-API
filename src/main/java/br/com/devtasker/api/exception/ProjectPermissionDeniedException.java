package br.com.devtasker.api.exception;

public class ProjectPermissionDeniedException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProjectPermissionDeniedException() {
        super("Você não possui permissão para modificar este projeto.");
    }
}
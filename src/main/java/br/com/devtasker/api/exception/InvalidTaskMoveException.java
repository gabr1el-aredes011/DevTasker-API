package br.com.devtasker.api.exception;

public class InvalidTaskMoveException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskMoveException(String message) {
        super(message);
    }
}
package br.com.devtasker.api.exception;

public class InvalidTaskAssigneeException extends RuntimeException {

    public InvalidTaskAssigneeException() {
        super("Os responsáveis precisam ser participantes ativos com permissão operacional no projeto.");
    }
}

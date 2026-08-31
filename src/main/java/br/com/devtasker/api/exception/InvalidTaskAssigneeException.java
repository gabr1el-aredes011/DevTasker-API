package br.com.devtasker.api.exception;

public class InvalidTaskAssigneeException extends RuntimeException {

    public InvalidTaskAssigneeException() {
        super("O responsável precisa ser um participante ativo com permissão operacional no projeto.");
    }
}

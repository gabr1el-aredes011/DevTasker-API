package br.com.devtasker.api.exception;

public class TaskCommentNotFoundException extends RuntimeException {

    public TaskCommentNotFoundException() {
        super("O comentário não foi encontrado.");
    }
}

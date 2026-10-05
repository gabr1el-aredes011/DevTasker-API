package br.com.devtasker.api.exception;

public class TaskChecklistItemNotFoundException extends RuntimeException {

    public TaskChecklistItemNotFoundException() {
        super("O item da checklist não foi encontrado.");
    }
}

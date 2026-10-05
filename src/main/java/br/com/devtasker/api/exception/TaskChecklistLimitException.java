package br.com.devtasker.api.exception;

public class TaskChecklistLimitException extends RuntimeException {

    public TaskChecklistLimitException() {
        super("Uma tarefa pode possuir no máximo 50 itens na checklist.");
    }
}

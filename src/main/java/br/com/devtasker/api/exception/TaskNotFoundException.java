package br.com.devtasker.api.exception;

public class TaskNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaskNotFoundException() {
        super("Tarefa não encontrada.");
    }
}
package br.com.devtasker.api.exception;

public class TaskAttachmentNotFoundException extends RuntimeException {

    public TaskAttachmentNotFoundException() {
        super("Anexo não encontrado.");
    }
}

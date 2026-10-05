package br.com.devtasker.api.exception;

import org.springframework.http.HttpStatus;

public class TaskAttachmentException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    private TaskAttachmentException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static TaskAttachmentException invalid(String message) {
        return new TaskAttachmentException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TASK_ATTACHMENT",
                message
        );
    }

    public static TaskAttachmentException tooLarge() {
        return new TaskAttachmentException(
                HttpStatus.CONTENT_TOO_LARGE,
                "TASK_ATTACHMENT_TOO_LARGE",
                "O anexo deve possuir no máximo 10 MB."
        );
    }

    public static TaskAttachmentException limitExceeded() {
        return new TaskAttachmentException(
                HttpStatus.BAD_REQUEST,
                "TASK_ATTACHMENT_LIMIT_EXCEEDED",
                "A tarefa pode possuir no máximo 10 anexos ativos."
        );
    }

    public static TaskAttachmentException storageFailure() {
        return new TaskAttachmentException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "TASK_ATTACHMENT_STORAGE_FAILURE",
                "Não foi possível acessar o arquivo do anexo."
        );
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

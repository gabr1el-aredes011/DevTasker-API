package br.com.devtasker.api.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ProjectLabelException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ProjectLabelException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}

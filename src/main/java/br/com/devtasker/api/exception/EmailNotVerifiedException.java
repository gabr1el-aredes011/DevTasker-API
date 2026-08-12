package br.com.devtasker.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class EmailNotVerifiedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailNotVerifiedException() {
        super(
            "Confirme seu e-mail antes de acessar o DevTasker."
        );
    }
}
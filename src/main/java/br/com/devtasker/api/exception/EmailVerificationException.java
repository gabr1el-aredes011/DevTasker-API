package br.com.devtasker.api.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String errorCode;

    private EmailVerificationException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);

        this.status = status;
        this.errorCode = errorCode;
    }

    public static EmailVerificationException invalidCode() {
        return new EmailVerificationException(
                HttpStatus.BAD_REQUEST,
                "INVALID_VERIFICATION_CODE",
                "O código de verificação é inválido."
        );
    }

    public static EmailVerificationException expiredCode() {
        return new EmailVerificationException(
                HttpStatus.GONE,
                "VERIFICATION_CODE_EXPIRED",
                "O código de verificação expirou. Solicite um novo código."
        );
    }

    public static EmailVerificationException attemptsExhausted() {
        return new EmailVerificationException(
                HttpStatus.TOO_MANY_REQUESTS,
                "VERIFICATION_ATTEMPTS_EXHAUSTED",
                "O limite de tentativas foi atingido. Solicite um novo código."
        );
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

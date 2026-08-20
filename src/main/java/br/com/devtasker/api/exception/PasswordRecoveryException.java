package br.com.devtasker.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordRecoveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String errorCode;

    private PasswordRecoveryException(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static PasswordRecoveryException invalid() {
        return new PasswordRecoveryException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_RECOVERY_INVALID",
                "Não foi possível concluir a recuperação de senha. "
                + "Solicite um novo código e tente novamente."
        );
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

package br.com.devtasker.api.email.exception;

public class EmailDeliveryException
        extends RuntimeException {

    private static final long serialVersionUID =
            1L;

    public EmailDeliveryException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
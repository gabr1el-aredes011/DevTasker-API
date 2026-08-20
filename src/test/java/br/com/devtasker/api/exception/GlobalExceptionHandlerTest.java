package br.com.devtasker.api.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import br.com.devtasker.api.email.exception.EmailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);

        when(request.getRequestURI())
                .thenReturn("/api/auth/login");
    }

    @Test
    void shouldReturnStandardErrorForUnverifiedEmail() {
        ResponseEntity<ApiError> response =
                handler.handleEmailNotVerified(
                        new EmailNotVerifiedException(),
                        request
                );

        ApiError error = response.getBody();

        assertAll(
                () -> assertEquals(
                        HttpStatus.FORBIDDEN,
                        response.getStatusCode()
                ),
                () -> assertEquals(
                        "EMAIL_NOT_VERIFIED",
                        error.error()
                ),
                () -> assertEquals(
                        "/api/auth/login",
                        error.path()
                )
        );
    }

    @Test
    void shouldReturnStandardErrorForVerificationFailure() {
        ResponseEntity<ApiError> response =
                handler.handleEmailVerification(
                        EmailVerificationException.expiredCode(),
                        request
                );

        ApiError error = response.getBody();

        assertAll(
                () -> assertEquals(
                        HttpStatus.GONE,
                        response.getStatusCode()
                ),
                () -> assertEquals(
                        "VERIFICATION_CODE_EXPIRED",
                        error.error()
                )
        );
    }

    @Test
    void shouldReturnGenericErrorForPasswordRecoveryFailure() {
        PasswordRecoveryException exception =
                mock(PasswordRecoveryException.class);

        when(exception.getStatus())
                .thenReturn(HttpStatus.BAD_REQUEST);

        when(exception.getErrorCode())
                .thenReturn("PASSWORD_RECOVERY_FAILED");

        when(exception.getMessage())
                .thenReturn("Estado interno sensível");

        ResponseEntity<ApiError> response =
                handler.handlePasswordRecovery(
                        exception,
                        request
                );

        ApiError error = response.getBody();

        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST,
                        response.getStatusCode()
                ),
                () -> assertEquals(
                        "PASSWORD_RECOVERY_FAILED",
                        error.error()
                ),
                () -> assertEquals(
                        "Não foi possível concluir "
                        + "a recuperação de senha.",
                        error.message()
                )
        );
    }

    @Test
    void shouldReturnServiceUnavailableWhenEmailDeliveryFails() {
        ResponseEntity<ApiError> response =
                handler.handleEmailDelivery(
                        new EmailDeliveryException(
                                "Falha SMTP",
                                new RuntimeException()
                        ),
                        request
                );

        ApiError error = response.getBody();

        assertAll(
                () -> assertEquals(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        response.getStatusCode()
                ),
                () -> assertEquals(
                        "EMAIL_DELIVERY_FAILED",
                        error.error()
                )
        );
    }
}

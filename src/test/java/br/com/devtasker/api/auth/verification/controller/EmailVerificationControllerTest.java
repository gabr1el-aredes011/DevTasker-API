package br.com.devtasker.api.auth.verification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import br.com.devtasker.api.auth.verification.dto.VerifyEmailRequest;
import br.com.devtasker.api.auth.verification.service.EmailVerificationResult;
import br.com.devtasker.api.auth.verification.service.EmailVerificationService;
import br.com.devtasker.api.exception.EmailVerificationException;

@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

    private static final String EMAIL =
            "gabriel@example.com";

    private static final String CODE =
            "483921";

    @Mock
    private EmailVerificationService emailVerificationService;

    private EmailVerificationController controller;

    @BeforeEach
    void setUp() {
        controller = new EmailVerificationController(
                emailVerificationService
        );
    }

    @Test
    void shouldReturnNoContentForVerifiedEmail() {
        when(emailVerificationService.confirm(EMAIL, CODE))
                .thenReturn(EmailVerificationResult.VERIFIED);

        ResponseEntity<Void> response = controller.confirm(
                new VerifyEmailRequest(EMAIL, CODE)
        );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );
    }

    @Test
    void shouldMapInvalidCodeToStandardException() {
        when(emailVerificationService.confirm(EMAIL, CODE))
                .thenReturn(EmailVerificationResult.INVALID_CODE);

        EmailVerificationException exception = assertThrows(
                EmailVerificationException.class,
                () -> controller.confirm(
                        new VerifyEmailRequest(EMAIL, CODE)
                )
        );

        assertEquals(
                "INVALID_VERIFICATION_CODE",
                exception.getErrorCode()
        );
    }

    @Test
    void shouldMapExpiredCodeToStandardException() {
        when(emailVerificationService.confirm(EMAIL, CODE))
                .thenReturn(EmailVerificationResult.EXPIRED);

        EmailVerificationException exception = assertThrows(
                EmailVerificationException.class,
                () -> controller.confirm(
                        new VerifyEmailRequest(EMAIL, CODE)
                )
        );

        assertEquals(
                HttpStatus.GONE,
                exception.getStatus()
        );
    }

    @Test
    void shouldMapExhaustedAttemptsToStandardException() {
        when(emailVerificationService.confirm(EMAIL, CODE))
                .thenReturn(
                        EmailVerificationResult.ATTEMPTS_EXHAUSTED
                );

        EmailVerificationException exception = assertThrows(
                EmailVerificationException.class,
                () -> controller.confirm(
                        new VerifyEmailRequest(EMAIL, CODE)
                )
        );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatus()
        );
    }
}

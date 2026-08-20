package br.com.devtasker.api.auth.passwordrecovery.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import br.com.devtasker.api.auth.passwordrecovery.dto.PasswordRecoveryChallengeResponse;
import br.com.devtasker.api.auth.passwordrecovery.dto.RequestPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.ResendPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.ResetPasswordRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.VerifyPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.VerifyPasswordRecoveryResponse;
import br.com.devtasker.api.auth.passwordrecovery.service.PasswordRecoveryFingerprintService;
import br.com.devtasker.api.auth.passwordrecovery.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;

class PasswordRecoveryControllerTest {

    private PasswordRecoveryService service;
    private PasswordRecoveryFingerprintService fingerprintService;
    private HttpServletRequest servletRequest;
    private PasswordRecoveryController controller;

    @BeforeEach
    void setUp() {
        service = mock(PasswordRecoveryService.class);
        fingerprintService = mock(PasswordRecoveryFingerprintService.class);
        servletRequest = mock(HttpServletRequest.class);
        when(fingerprintService.clientFingerprint(servletRequest))
                .thenReturn("client-fingerprint");
        controller = new PasswordRecoveryController(
                service,
                fingerprintService
        );
    }

    @Test
    void shouldReturnAcceptedChallengeForRequest() {
        UUID challengeId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.of(
                2026,
                8,
                20,
                15,
                10,
                0,
                0,
                ZoneOffset.UTC
        );
        PasswordRecoveryChallengeResponse expected =
                new PasswordRecoveryChallengeResponse(
                        challengeId,
                        expiresAt,
                        expiresAt.minusMinutes(9)
                );
        when(service.request(
                "user@devtasker.test",
                "client-fingerprint"
        ))
                .thenReturn(expected);

        ResponseEntity<PasswordRecoveryChallengeResponse> response =
                controller.request(
                        new RequestPasswordRecoveryRequest(
                                "user@devtasker.test"
                        ),
                        servletRequest
                );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturnAcceptedForResend() {
        ResponseEntity<Void> response = controller.resend(
                new ResendPasswordRecoveryRequest("challenge-id"),
                servletRequest
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
        verify(service).resend("challenge-id", "client-fingerprint");
    }

    @Test
    void shouldKeepRequestAndResendNeutralForNullJsonPayload() {
        PasswordRecoveryChallengeResponse expected =
                new PasswordRecoveryChallengeResponse(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10),
                        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1)
                );
        when(service.request(null, "client-fingerprint"))
                .thenReturn(expected);

        ResponseEntity<PasswordRecoveryChallengeResponse> requestResponse =
                controller.request(null, servletRequest);
        ResponseEntity<Void> resendResponse =
                controller.resend(null, servletRequest);

        assertEquals(HttpStatus.ACCEPTED, requestResponse.getStatusCode());
        assertEquals(expected, requestResponse.getBody());
        assertEquals(HttpStatus.ACCEPTED, resendResponse.getStatusCode());
        verify(service).resend(null, "client-fingerprint");
    }

    @Test
    void shouldReturnResetTokenForVerifiedCode() {
        VerifyPasswordRecoveryResponse expected =
                new VerifyPasswordRecoveryResponse(
                        "reset-token",
                        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)
                );
        when(service.verify(
                "challenge-id",
                "123456",
                "client-fingerprint"
        ))
                .thenReturn(expected);

        ResponseEntity<VerifyPasswordRecoveryResponse> response =
                controller.verify(
                        new VerifyPasswordRecoveryRequest(
                                "challenge-id",
                                "123456"
                        ),
                        servletRequest
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void shouldReturnNoContentAfterPasswordReset() {
        ResponseEntity<Void> response = controller.reset(
                new ResetPasswordRequest(
                        "reset-token",
                        "new-password"
                ),
                servletRequest
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(service).reset(
                "reset-token",
                "new-password",
                "client-fingerprint"
        );
    }
}

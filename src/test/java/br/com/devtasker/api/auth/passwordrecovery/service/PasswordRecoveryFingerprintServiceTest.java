package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class PasswordRecoveryFingerprintServiceTest {

    @Test
    void shouldCreateStableScopedFingerprintWithoutRawAddress() {
        PasswordRecoveryFingerprintService service =
                new PasswordRecoveryFingerprintService();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");

        String first = service.clientFingerprint(request);
        String second = service.clientFingerprint(request);
        String identifier = service.identifierFingerprint(
                "email",
                "203.0.113.10"
        );

        assertEquals(first, second);
        assertNotEquals(first, identifier);
        assertFalse(first.contains("203.0.113.10"));
    }
}

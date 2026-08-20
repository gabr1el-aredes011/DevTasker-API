package br.com.devtasker.api.auth.passwordrecovery.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class PasswordRecoveryFingerprintService {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 32;

    private final byte[] processSalt;

    public PasswordRecoveryFingerprintService() {
        this.processSalt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(processSalt);
    }

    public String clientFingerprint(HttpServletRequest request) {
        String remoteAddress = request == null
                ? "unknown"
                : request.getRemoteAddr();

        return fingerprint("client", remoteAddress);
    }

    public String identifierFingerprint(
            String namespace,
            String identifier
    ) {
        return fingerprint(namespace, identifier);
    }

    private String fingerprint(String namespace, String value) {
        MessageDigest digest = newDigest();
        digest.update(processSalt);
        digest.update(requireValue(namespace).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(requireValue(value).getBytes(StandardCharsets.UTF_8));

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não está disponível.",
                    exception
            );
        }
    }

    private static String requireValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}

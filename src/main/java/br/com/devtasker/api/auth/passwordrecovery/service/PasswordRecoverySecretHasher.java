package br.com.devtasker.api.auth.passwordrecovery.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import br.com.devtasker.api.auth.passwordrecovery.config.PasswordRecoveryProperties;

@Component
public class PasswordRecoverySecretHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String CODE_PURPOSE =
            "devtasker:password-recovery-code:v1:";
    private static final String TOKEN_PURPOSE =
            "devtasker:password-recovery-token:v1:";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public PasswordRecoverySecretHasher(
            PasswordRecoveryProperties properties
    ) {
        byte[] secretBytes = decodeSecret(properties.hmacSecret());

        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "PASSWORD_RECOVERY_HMAC_SECRET "
                    + "deve possuir pelo menos 32 bytes."
            );
        }

        this.secretKey = new SecretKeySpec(secretBytes, ALGORITHM);
    }

    public String hashCode(Long userId, String rawCode) {
        validateUserId(userId);
        validateSecret(rawCode, "O código é obrigatório.");

        return encode(calculate(CODE_PURPOSE + userId + ":" + rawCode));
    }

    public boolean codeMatches(
            Long userId,
            String rawCode,
            String storedHash
    ) {
        byte[] calculated = decode(hashCode(userId, rawCode));
        byte[] expected = decodeStoredHash(storedHash);
        return MessageDigest.isEqual(calculated, expected);
    }

    public String hashResetToken(String rawToken) {
        validateSecret(rawToken, "O token é obrigatório.");
        return encode(calculate(TOKEN_PURPOSE + rawToken));
    }

    private byte[] calculate(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);

            return mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Não foi possível proteger a credencial de recuperação.",
                    exception
            );
        }
    }

    private static String encode(byte[] value) {
        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private static byte[] decodeStoredHash(String storedHash) {
        validateSecret(storedHash, "O hash armazenado é obrigatório.");

        try {
            return decode(storedHash);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "O hash armazenado possui formato inválido.",
                    exception
            );
        }
    }

    private static byte[] decodeSecret(String encodedSecret) {
        try {
            return Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "PASSWORD_RECOVERY_HMAC_SECRET deve estar em Base64.",
                    exception
            );
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "O identificador do usuário é obrigatório."
            );
        }
    }

    private static void validateSecret(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}

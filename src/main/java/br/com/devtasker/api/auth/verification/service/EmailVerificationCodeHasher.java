package br.com.devtasker.api.auth.verification.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;

@Component
public class EmailVerificationCodeHasher {

    private static final String ALGORITHM =
            "HmacSHA256";

    private static final String PURPOSE =
            "devtasker:email-verification:v1:";

    private static final int MINIMUM_SECRET_BYTES =
            32;

    private final SecretKey secretKey;

    public EmailVerificationCodeHasher(
            EmailVerificationProperties properties
    ) {
        byte[] secretBytes =
                decodeSecret(
                        properties.hmacSecret()
                );

        if (
                secretBytes.length <
                MINIMUM_SECRET_BYTES
        ) {
            throw new IllegalStateException(
                    "EMAIL_VERIFICATION_HMAC_SECRET "
                    + "deve possuir pelo menos 32 bytes."
            );
        }

        this.secretKey = new SecretKeySpec(
                secretBytes,
                ALGORITHM
        );
    }

    public String hash(
            Long userId,
            String rawCode
    ) {
        byte[] result =
                calculate(userId, rawCode);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(result);
    }

    public boolean matches(
            Long userId,
            String rawCode,
            String storedHash
    ) {
        validateStoredHash(storedHash);

        byte[] calculatedHash =
                calculate(userId, rawCode);

        byte[] expectedHash;

        try {
            expectedHash = Base64
                    .getUrlDecoder()
                    .decode(storedHash);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "O hash do código armazenado "
                    + "possui formato inválido.",
                    exception
            );
        }

        return MessageDigest.isEqual(
                calculatedHash,
                expectedHash
        );
    }

    private byte[] calculate(
            Long userId,
            String rawCode
    ) {
        validateUserId(userId);
        validateRawCode(rawCode);

        String payload =
                PURPOSE
                + userId
                + ":"
                + rawCode;

        try {
            Mac mac =
                    Mac.getInstance(ALGORITHM);

            mac.init(secretKey);

            return mac.doFinal(
                    payload.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Não foi possível proteger "
                    + "o código de verificação.",
                    exception
            );
        }
    }

    private static byte[] decodeSecret(
            String encodedSecret
    ) {
        try {
            return Base64
                    .getDecoder()
                    .decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "EMAIL_VERIFICATION_HMAC_SECRET "
                    + "deve estar em Base64.",
                    exception
            );
        }
    }

    private static void validateUserId(
            Long userId
    ) {
        if (
                userId == null ||
                userId <= 0
        ) {
            throw new IllegalArgumentException(
                    "O identificador do usuário "
                    + "é obrigatório."
            );
        }
    }

    private static void validateRawCode(
            String rawCode
    ) {
        if (
                rawCode == null ||
                rawCode.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O código de verificação "
                    + "é obrigatório."
            );
        }
    }

    private static void validateStoredHash(
            String storedHash
    ) {
        if (
                storedHash == null ||
                storedHash.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "O hash armazenado é obrigatório."
            );
        }
    }
}
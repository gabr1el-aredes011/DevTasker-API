package br.com.devtasker.api.auth.passwordrecovery.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.devtasker.api.auth.passwordrecovery.config.PasswordRecoveryProperties;

@Component
public class PasswordRecoverySecretGenerator {

    private static final int DECIMAL_BASE = 10;
    private static final int RESET_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom;
    private final PasswordRecoveryProperties properties;

    @Autowired
    public PasswordRecoverySecretGenerator(
            PasswordRecoveryProperties properties
    ) {
        this(new SecureRandom(), properties);
    }

    PasswordRecoverySecretGenerator(
            SecureRandom secureRandom,
            PasswordRecoveryProperties properties
    ) {
        this.secureRandom = secureRandom;
        this.properties = properties;
    }

    public String generateCode() {
        int codeLength = properties.codeLength();
        StringBuilder code = new StringBuilder(codeLength);

        for (int index = 0; index < codeLength; index++) {
            code.append(secureRandom.nextInt(DECIMAL_BASE));
        }

        return code.toString();
    }

    public String generateResetToken() {
        byte[] token = new byte[RESET_TOKEN_BYTES];
        secureRandom.nextBytes(token);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(token);
    }
}

package br.com.devtasker.api.auth.verification.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;

@Component
public class EmailVerificationCodeGenerator {

    private static final int DECIMAL_BASE = 10;

    private final SecureRandom secureRandom;
    private final EmailVerificationProperties properties;

    public EmailVerificationCodeGenerator(
            SecureRandom secureRandom,
            EmailVerificationProperties properties
    ) {
        this.secureRandom = secureRandom;
        this.properties = properties;
    }

    public String generate() {
        int codeLength = properties.codeLength();

        StringBuilder code =
                new StringBuilder(codeLength);

        for (
                int index = 0;
                index < codeLength;
                index++
        ) {
            int digit =
                    secureRandom.nextInt(DECIMAL_BASE);

            code.append(digit);
        }

        return code.toString();
    }
}
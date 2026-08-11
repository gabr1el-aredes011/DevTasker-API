package br.com.devtasker.api.auth.verification.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;
import br.com.devtasker.api.auth.verification.domain.EmailVerificationCode;
import br.com.devtasker.api.auth.verification.repository.EmailVerificationCodeRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class EmailVerificationCodeService {

    private final EmailVerificationCodeRepository
            emailVerificationCodeRepository;

    private final EmailVerificationCodeGenerator
            codeGenerator;

    private final EmailVerificationCodeHasher
            codeHasher;

    private final EmailVerificationProperties
            properties;

    public EmailVerificationCodeService(
            EmailVerificationCodeRepository
                    emailVerificationCodeRepository,
            EmailVerificationCodeGenerator
                    codeGenerator,
            EmailVerificationCodeHasher
                    codeHasher,
            EmailVerificationProperties
                    properties
    ) {
        this.emailVerificationCodeRepository =
                emailVerificationCodeRepository;

        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.properties = properties;
    }

    @Transactional
    public IssuedEmailVerificationCode issueFor(
            UserAccount user
    ) {
        validateUser(user);

        if (user.isEmailVerified()) {
            throw new IllegalStateException(
                    "O e-mail deste usuário "
                    + "já está verificado."
            );
        }

        String rawCode =
                codeGenerator.generate();

        String codeHash =
                codeHasher.hash(
                        user.getId(),
                        rawCode
                );

        OffsetDateTime expiresAt =
                OffsetDateTime
                        .now(ZoneOffset.UTC)
                        .plus(
                                properties
                                        .expirationDuration()
                        );

        EmailVerificationCode verificationCode =
                emailVerificationCodeRepository
                        .findByUserId(user.getId())
                        .map(existingCode -> {
                            existingCode.renew(
                                    codeHash,
                                    expiresAt
                            );

                            return existingCode;
                        })
                        .orElseGet(() ->
                                EmailVerificationCode
                                        .create(
                                                user,
                                                codeHash,
                                                expiresAt
                                        )
                        );

        emailVerificationCodeRepository.save(
                verificationCode
        );

        return new IssuedEmailVerificationCode(
                rawCode,
                expiresAt
        );
    }

    public boolean matches(
            EmailVerificationCode verificationCode,
            String rawCode
    ) {
        if (verificationCode == null) {
            throw new IllegalArgumentException(
                    "A verificação é obrigatória."
            );
        }

        return codeHasher.matches(
                verificationCode
                        .getUser()
                        .getId(),

                rawCode,

                verificationCode
                        .getCodeHash()
        );
    }

    private static void validateUser(
            UserAccount user
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "O usuário é obrigatório."
            );
        }

        if (user.getId() == null) {
            throw new IllegalStateException(
                    "O usuário precisa estar salvo "
                    + "antes da emissão do código."
            );
        }
    }
}
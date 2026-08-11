package br.com.devtasker.api.auth.verification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;
import br.com.devtasker.api.email.model.VerificationEmailMessage;
import br.com.devtasker.api.email.service.VerificationEmailSender;
import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class EmailVerificationDeliveryService {

    private final EmailVerificationCodeService
            emailVerificationCodeService;

    private final VerificationEmailSender
            verificationEmailSender;

    private final EmailVerificationProperties
            properties;

    public EmailVerificationDeliveryService(
            EmailVerificationCodeService
                    emailVerificationCodeService,
            VerificationEmailSender
                    verificationEmailSender,
            EmailVerificationProperties properties
    ) {
        this.emailVerificationCodeService =
                emailVerificationCodeService;

        this.verificationEmailSender =
                verificationEmailSender;

        this.properties = properties;
    }

    @Transactional
    public IssuedEmailVerificationCode issueAndSend(
            UserAccount user
    ) {
        IssuedEmailVerificationCode issuedCode =
                emailVerificationCodeService
                        .issueFor(user);

        VerificationEmailMessage message =
                new VerificationEmailMessage(
                        user.getName(),
                        user.getEmail(),
                        issuedCode.rawCode(),
                        properties.expirationMinutes()
                );

        verificationEmailSender.send(message);

        return issuedCode;
    }
}
package br.com.devtasker.api.email.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;

import br.com.devtasker.api.email.config.ApplicationMailProperties;
import br.com.devtasker.api.email.exception.EmailDeliveryException;
import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.template.PasswordRecoveryEmailTemplate;

class SmtpPasswordRecoveryEmailSenderTest {

    @Test
    void shouldWrapFailureWhileCreatingMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenThrow(new MailPreparationException("failed"));
        SmtpPasswordRecoveryEmailSender sender =
                new SmtpPasswordRecoveryEmailSender(
                        mailSender,
                        new ApplicationMailProperties(
                                "no-reply@devtasker.test",
                                "DevTasker"
                        ),
                        new PasswordRecoveryEmailTemplate()
                );

        assertThrows(
                EmailDeliveryException.class,
                () -> sender.send(
                        new PasswordRecoveryEmailMessage(
                                "Dev User",
                                "user@devtasker.test",
                                "123456",
                                10
                        )
                )
        );
    }
}

package br.com.devtasker.api.email.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import br.com.devtasker.api.email.config.ApplicationMailProperties;
import br.com.devtasker.api.email.model.VerificationEmailMessage;
import br.com.devtasker.api.email.template.VerificationEmailTemplate;

@EnabledIfEnvironmentVariable(
        named = "EMAIL_SMOKE_TEST_RECIPIENT",
        matches = ".+@.+"
)
class VerificationEmailSenderSmokeTest {

    @Test
    void shouldSendVerificationEmail() {
        JavaMailSenderImpl mailSender =
                createMailSender();

        ApplicationMailProperties mailProperties =
                new ApplicationMailProperties(
                        requiredEnvironmentVariable(
                                "MAIL_FROM_ADDRESS"
                        ),
                        optionalEnvironmentVariable(
                                "MAIL_FROM_NAME",
                                "DevTasker"
                        )
                );

        VerificationEmailSender emailSender =
                new SmtpVerificationEmailSender(
                        mailSender,
                        mailProperties,
                        new VerificationEmailTemplate()
                );

        VerificationEmailMessage message =
                new VerificationEmailMessage(
                        "Gabriel",
                        requiredEnvironmentVariable(
                                "EMAIL_SMOKE_TEST_RECIPIENT"
                        ),
                        "483921",
                        10
                );

        assertDoesNotThrow(
                () -> emailSender.send(message)
        );
    }

    private static JavaMailSenderImpl
            createMailSender() {

        JavaMailSenderImpl mailSender =
                new JavaMailSenderImpl();

        mailSender.setHost(
                optionalEnvironmentVariable(
                        "MAIL_HOST",
                        "smtp.gmail.com"
                )
        );

        mailSender.setPort(
                Integer.parseInt(
                        optionalEnvironmentVariable(
                                "MAIL_PORT",
                                "587"
                        )
                )
        );

        mailSender.setUsername(
                requiredEnvironmentVariable(
                        "MAIL_USERNAME"
                )
        );

        mailSender.setPassword(
                requiredEnvironmentVariable(
                        "MAIL_PASSWORD"
                )
        );

        mailSender.setDefaultEncoding(
                StandardCharsets.UTF_8.name()
        );

        Properties properties =
                mailSender.getJavaMailProperties();

        properties.put(
                "mail.transport.protocol",
                "smtp"
        );

        properties.put(
                "mail.smtp.auth",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.required",
                "true"
        );

        properties.put(
                "mail.smtp.connectiontimeout",
                "5000"
        );

        properties.put(
                "mail.smtp.timeout",
                "5000"
        );

        properties.put(
                "mail.smtp.writetimeout",
                "5000"
        );

        return mailSender;
    }

    private static String
            requiredEnvironmentVariable(
                    String variableName
            ) {

        String value =
                System.getenv(variableName);

        if (
                value == null ||
                value.isBlank()
        ) {
            throw new IllegalStateException(
                    "A variável de ambiente "
                    + variableName
                    + " não foi configurada."
            );
        }

        return value;
    }

    private static String
            optionalEnvironmentVariable(
                    String variableName,
                    String defaultValue
            ) {

        String value =
                System.getenv(variableName);

        if (
                value == null ||
                value.isBlank()
        ) {
            return defaultValue;
        }

        return value;
    }
}
package br.com.devtasker.api.email.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import br.com.devtasker.api.email.config.ApplicationMailProperties;
import br.com.devtasker.api.email.exception.EmailDeliveryException;
import br.com.devtasker.api.email.model.RenderedEmail;
import br.com.devtasker.api.email.model.VerificationEmailMessage;
import br.com.devtasker.api.email.template.VerificationEmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpVerificationEmailSender
        implements VerificationEmailSender {

    private final JavaMailSender mailSender;

    private final ApplicationMailProperties
            mailProperties;

    private final VerificationEmailTemplate
            emailTemplate;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            ApplicationMailProperties mailProperties,
            VerificationEmailTemplate emailTemplate
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.emailTemplate = emailTemplate;
    }

    @Override
    public void send(
            VerificationEmailMessage message
    ) {
        RenderedEmail renderedEmail =
                emailTemplate.render(message);

        MimeMessage mimeMessage =
                mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mimeMessage,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    mailProperties.fromAddress(),
                    mailProperties.fromName()
            );

            helper.setTo(
                    message.recipientEmail()
            );

            helper.setSubject(
                    renderedEmail.subject()
            );

            helper.setText(
                    renderedEmail.plainText(),
                    renderedEmail.htmlText()
            );

            mailSender.send(mimeMessage);

        } catch (
                MessagingException |
                UnsupportedEncodingException exception
        ) {
            throw new EmailDeliveryException(
                    "Não foi possível preparar "
                    + "o e-mail de verificação.",
                    exception
            );

        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Não foi possível enviar "
                    + "o e-mail de verificação.",
                    exception
            );
        }
    }
}
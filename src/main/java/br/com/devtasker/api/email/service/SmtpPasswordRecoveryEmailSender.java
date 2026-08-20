package br.com.devtasker.api.email.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import br.com.devtasker.api.email.config.ApplicationMailProperties;
import br.com.devtasker.api.email.exception.EmailDeliveryException;
import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.model.RenderedEmail;
import br.com.devtasker.api.email.template.PasswordRecoveryEmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpPasswordRecoveryEmailSender
        implements PasswordRecoveryEmailSender {

    private final JavaMailSender mailSender;
    private final ApplicationMailProperties mailProperties;
    private final PasswordRecoveryEmailTemplate emailTemplate;

    public SmtpPasswordRecoveryEmailSender(
            JavaMailSender mailSender,
            ApplicationMailProperties mailProperties,
            PasswordRecoveryEmailTemplate emailTemplate
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.emailTemplate = emailTemplate;
    }

    @Override
    public void send(PasswordRecoveryEmailMessage message) {
        try {
            RenderedEmail renderedEmail = emailTemplate.render(message);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(
                    mailProperties.fromAddress(),
                    mailProperties.fromName()
            );
            helper.setTo(message.recipientEmail());
            helper.setSubject(renderedEmail.subject());
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
                    "Não foi possível preparar o e-mail de recuperação.",
                    exception
            );
        } catch (MailException exception) {
            throw new EmailDeliveryException(
                    "Não foi possível enviar o e-mail de recuperação.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException(
                    "Não foi possível preparar o e-mail de recuperação.",
                    exception
            );
        }
    }
}

package br.com.devtasker.api.email.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import br.com.devtasker.api.email.config.ApplicationMailProperties;
import br.com.devtasker.api.email.exception.EmailDeliveryException;
import br.com.devtasker.api.email.model.ProjectInvitationEmailMessage;
import br.com.devtasker.api.email.model.RenderedEmail;
import br.com.devtasker.api.email.template.ProjectInvitationEmailTemplate;
import jakarta.mail.MessagingException;

@Service
public class SmtpProjectInvitationEmailSender implements ProjectInvitationEmailSender {

    private final JavaMailSender mailSender;
    private final ApplicationMailProperties mailProperties;
    private final ProjectInvitationEmailTemplate template;

    public SmtpProjectInvitationEmailSender(
            JavaMailSender mailSender,
            ApplicationMailProperties mailProperties,
            ProjectInvitationEmailTemplate template
    ) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.template = template;
    }

    @Override
    public void send(ProjectInvitationEmailMessage message) {
        RenderedEmail rendered = template.render(message);
        var mimeMessage = mailSender.createMimeMessage();

        try {
            var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.fromAddress(), mailProperties.fromName());
            helper.setTo(message.recipientEmail());
            helper.setSubject(rendered.subject());
            helper.setText(rendered.plainText(), rendered.htmlText());
            mailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            throw new EmailDeliveryException("Não foi possível enviar o convite do projeto.", exception);
        }
    }
}

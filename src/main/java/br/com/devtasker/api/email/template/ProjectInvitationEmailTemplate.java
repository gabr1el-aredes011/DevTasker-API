package br.com.devtasker.api.email.template;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import br.com.devtasker.api.email.model.ProjectInvitationEmailMessage;
import br.com.devtasker.api.email.model.RenderedEmail;

@Component
public class ProjectInvitationEmailTemplate {

    public RenderedEmail render(ProjectInvitationEmailMessage message) {
        String safeName = HtmlUtils.htmlEscape(message.recipientName());
        String safeInviter = HtmlUtils.htmlEscape(message.inviterName());
        String safeProject = HtmlUtils.htmlEscape(message.projectName());
        String safeRole = HtmlUtils.htmlEscape(message.roleLabel());
        String safeUrl = HtmlUtils.htmlEscape(message.acceptanceUrl());

        String subject = "Convite para participar de " + message.projectName() + " no DevTasker";
        String plainText = """
                Olá, %s!

                %s convidou você para participar do projeto %s como %s.

                Aceite o convite: %s

                Este convite expira em %d horas. Se você não esperava esta mensagem, ignore-a.

                DevTasker
                Complexidade em fluxo.
                """.formatted(
                message.recipientName(),
                message.inviterName(),
                message.projectName(),
                message.roleLabel(),
                message.acceptanceUrl(),
                message.expirationHours()
        );

        String htmlText = """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Convite DevTasker</title></head>
                <body style="margin:0;padding:0;background:#020805;color:#f4fff7;font-family:Arial,Helvetica,sans-serif">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="width:100%%;background:#020805;padding:40px 16px">
                    <tr><td align="center">
                      <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="width:100%%;max-width:560px;border:1px solid #17442a;border-radius:20px;background:#07150e">
                        <tr><td style="height:4px;background:#45ed78"></td></tr>
                        <tr><td style="padding:34px">
                          <div style="margin-bottom:28px;color:#59ff88;font-size:18px;font-weight:700">✓ DevTasker</div>
                          <div style="margin-bottom:10px;color:#45ed78;font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase">Colaboração</div>
                          <h1 style="margin:0 0 16px;color:#f4fff7;font-size:28px;line-height:1.2">Você recebeu um convite</h1>
                          <p style="margin:0 0 22px;color:#a9c8b3;font-size:15px;line-height:1.7">Olá, %s! <strong style="color:#d8ffe3">%s</strong> convidou você para participar de <strong style="color:#d8ffe3">%s</strong> como %s.</p>
                          <a href="%s" style="display:inline-block;padding:14px 20px;border-radius:10px;background:#45ed78;color:#021007;font-weight:700;text-decoration:none">Aceitar convite →</a>
                          <p style="margin:22px 0 0;color:#668372;font-size:12px;line-height:1.6">Este convite expira em %d horas. Se você não esperava esta mensagem, ignore-a.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                safeName,
                safeInviter,
                safeProject,
                safeRole,
                safeUrl,
                message.expirationHours()
        );

        return new RenderedEmail(subject, plainText, htmlText);
    }
}

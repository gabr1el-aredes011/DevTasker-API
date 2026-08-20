package br.com.devtasker.api.email.template;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.model.RenderedEmail;

@Component
public class PasswordRecoveryEmailTemplate {

    public RenderedEmail render(
            PasswordRecoveryEmailMessage message
    ) {
        String safeName = HtmlUtils.htmlEscape(message.recipientName());
        String safeCode = HtmlUtils.htmlEscape(message.recoveryCode());

        String plainText = """
                Olá, %s!

                Seu código para redefinir a senha do DevTasker é:

                %s

                Este código expira em %d minutos.

                Se você não solicitou a redefinição da senha,
                ignore esta mensagem. Não compartilhe este código.

                DevTasker
                Complexidade em fluxo.
                """.formatted(
                        message.recipientName(),
                        message.recoveryCode(),
                        message.expirationMinutes()
                );

        String htmlText = """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta
                        name="viewport"
                        content="width=device-width, initial-scale=1"
                    >
                    <title>Recuperação de senha DevTasker</title>
                </head>
                <body style="
                    margin: 0;
                    padding: 0;
                    background: #020805;
                    color: #f4fff7;
                    font-family: Arial, Helvetica, sans-serif;
                ">
                    <table
                        role="presentation"
                        width="100%%"
                        cellspacing="0"
                        cellpadding="0"
                        style="
                            width: 100%%;
                            background: #020805;
                            padding: 40px 16px;
                        "
                    >
                        <tr>
                            <td align="center">
                                <table
                                    role="presentation"
                                    width="560"
                                    cellspacing="0"
                                    cellpadding="0"
                                    style="
                                        width: 100%%;
                                        max-width: 560px;
                                        border: 1px solid #17442a;
                                        border-radius: 20px;
                                        background: #07150e;
                                    "
                                >
                                    <tr>
                                        <td style="
                                            height: 4px;
                                            background: #14d95f;
                                        "></td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 34px;">
                                            <div style="
                                                margin-bottom: 24px;
                                                color: #59ff88;
                                                font-size: 18px;
                                                font-weight: 700;
                                            ">
                                                DevTasker
                                            </div>
                                            <h1 style="
                                                margin: 0 0 16px;
                                                font-size: 28px;
                                            ">
                                                Redefina sua senha
                                            </h1>
                                            <p style="
                                                color: #a9c8b3;
                                                line-height: 1.7;
                                            ">
                                                Olá, %s! Use o código abaixo
                                                para continuar.
                                            </p>
                                            <div style="
                                                margin: 24px 0;
                                                padding: 24px 12px;
                                                border: 1px solid #1f5e37;
                                                border-radius: 14px;
                                                background: #020a06;
                                                color: #59ff88;
                                                font-family: Consolas, monospace;
                                                font-size: 36px;
                                                font-weight: 700;
                                                letter-spacing: 10px;
                                                text-align: center;
                                            ">
                                                %s
                                            </div>
                                            <p style="
                                                color: #8eaa97;
                                                font-size: 13px;
                                                line-height: 1.6;
                                            ">
                                                O código expira em
                                                <strong style="color: #d8ffe3;">
                                                    %d minutos
                                                </strong>.
                                                Se você não fez esta solicitação,
                                                ignore o e-mail.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                        safeName,
                        safeCode,
                        message.expirationMinutes()
                );

        return new RenderedEmail(
                "Redefina sua senha no DevTasker",
                plainText,
                htmlText
        );
    }
}

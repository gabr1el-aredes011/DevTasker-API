package br.com.devtasker.api.email.template;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import br.com.devtasker.api.email.model.RenderedEmail;
import br.com.devtasker.api.email.model.VerificationEmailMessage;

@Component
public class VerificationEmailTemplate {

    public RenderedEmail render(
            VerificationEmailMessage message
    ) {
        String safeName =
                HtmlUtils.htmlEscape(
                        message.recipientName()
                );

        String safeCode =
                HtmlUtils.htmlEscape(
                        message.verificationCode()
                );

        String subject =
                "Confirme seu e-mail no DevTasker";

        String plainText = """
                Olá, %s!

                Seu código de verificação do DevTasker é:

                %s

                Este código expira em %d minutos.

                Se você não criou uma conta no DevTasker,
                ignore esta mensagem.

                DevTasker
                Complexidade em fluxo.
                """.formatted(
                        message.recipientName(),
                        message.verificationCode(),
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
                    <title>Verificação DevTasker</title>
                </head>

                <body style="
                    margin: 0;
                    padding: 0;
                    background: #020805;
                    color: #f4fff7;
                    font-family:
                        Arial,
                        Helvetica,
                        sans-serif;
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
                                        overflow: hidden;
                                        border:
                                            1px solid #17442a;
                                        border-radius: 20px;
                                        background: #07150e;
                                    "
                                >
                                    <tr>
                                        <td style="
                                            height: 4px;
                                            background:
                                                linear-gradient(
                                                    90deg,
                                                    #14d95f,
                                                    #59ff88
                                                );
                                        "></td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding:
                                                34px 34px 18px;
                                        ">
                                            <div style="
                                                margin-bottom: 30px;
                                                color: #59ff88;
                                                font-size: 18px;
                                                font-weight: 700;
                                            ">
                                                ✓ DevTasker
                                            </div>

                                            <div style="
                                                margin-bottom: 10px;
                                                color: #45ed78;
                                                font-size: 11px;
                                                font-weight: 700;
                                                letter-spacing: 2px;
                                                text-transform: uppercase;
                                            ">
                                                Verificação de e-mail
                                            </div>

                                            <h1 style="
                                                margin: 0 0 16px;
                                                color: #f4fff7;
                                                font-size: 28px;
                                                line-height: 1.2;
                                            ">
                                                Confirme sua conta
                                            </h1>

                                            <p style="
                                                margin: 0;
                                                color: #a9c8b3;
                                                font-size: 15px;
                                                line-height: 1.7;
                                            ">
                                                Olá, %s! Use o código
                                                abaixo para confirmar
                                                que este endereço de
                                                e-mail pertence a você.
                                            </p>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding: 12px 34px 18px;
                                        ">
                                            <div style="
                                                padding: 24px 12px;
                                                border:
                                                    1px solid #1f5e37;
                                                border-radius: 14px;
                                                background: #020a06;
                                                color: #59ff88;
                                                font-family:
                                                    Consolas,
                                                    monospace;
                                                font-size: 36px;
                                                font-weight: 700;
                                                letter-spacing: 10px;
                                                text-align: center;
                                                text-shadow:
                                                    0 0 24px
                                                    rgba(
                                                        89,
                                                        255,
                                                        136,
                                                        0.32
                                                    );
                                            ">
                                                %s
                                            </div>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding: 0 34px 34px;
                                        ">
                                            <p style="
                                                margin: 0 0 22px;
                                                color: #8eaa97;
                                                font-size: 13px;
                                                line-height: 1.6;
                                            ">
                                                O código expira em
                                                <strong style="
                                                    color: #d8ffe3;
                                                ">
                                                    %d minutos
                                                </strong>.
                                                Não compartilhe este
                                                código com ninguém.
                                            </p>

                                            <div style="
                                                padding-top: 22px;
                                                border-top:
                                                    1px solid #163322;
                                                color: #668372;
                                                font-size: 12px;
                                                line-height: 1.6;
                                            ">
                                                Se você não criou uma
                                                conta no DevTasker,
                                                ignore esta mensagem.
                                            </div>
                                        </td>
                                    </tr>
                                </table>

                                <p style="
                                    margin: 20px 0 0;
                                    color: #486254;
                                    font-size: 11px;
                                ">
                                    DevTasker · Complexidade em fluxo.
                                </p>
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
                subject,
                plainText,
                htmlText
        );
    }
}
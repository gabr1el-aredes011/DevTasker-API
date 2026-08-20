package br.com.devtasker.api.email.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.model.RenderedEmail;

class PasswordRecoveryEmailTemplateTest {

    @Test
    void shouldEscapeUserControlledHtmlAndRenderRecoveryCode() {
        PasswordRecoveryEmailTemplate template =
                new PasswordRecoveryEmailTemplate();

        RenderedEmail rendered = template.render(
                new PasswordRecoveryEmailMessage(
                        "<script>alert(1)</script>",
                        "user@devtasker.test",
                        "123456",
                        10
                )
        );

        assertTrue(rendered.subject().contains("senha"));
        assertTrue(rendered.plainText().contains("123456"));
        assertTrue(rendered.htmlText().contains("123456"));
        assertFalse(rendered.htmlText().contains("<script>alert(1)</script>"));
    }
}

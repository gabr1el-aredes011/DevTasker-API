package br.com.devtasker.api.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;
import br.com.devtasker.api.email.model.VerificationEmailMessage;
import br.com.devtasker.api.email.service.VerificationEmailSender;
import br.com.devtasker.api.user.domain.UserAccount;

@ExtendWith(MockitoExtension.class)
class EmailVerificationDeliveryServiceTest {

    @Mock
    private EmailVerificationCodeService
            emailVerificationCodeService;

    @Mock
    private VerificationEmailSender
            verificationEmailSender;

    @Mock
    private UserAccount user;

    private EmailVerificationDeliveryService service;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties =
                new EmailVerificationProperties(
                        6,
                        10,
                        5,
                        60,
                        validSecret()
                );

        service =
                new EmailVerificationDeliveryService(
                        emailVerificationCodeService,
                        verificationEmailSender,
                        properties
                );
    }

    @Test
    void shouldIssueAndSendVerificationCode() {
        OffsetDateTime expiresAt =
                OffsetDateTime
                        .now(ZoneOffset.UTC)
                        .plusMinutes(10);

        IssuedEmailVerificationCode issuedCode =
                new IssuedEmailVerificationCode(
                        "483921",
                        expiresAt
                );

        when(user.getName())
                .thenReturn("Gabriel");

        when(user.getEmail())
                .thenReturn(
                        "gabriel@example.com"
                );

        when(
                emailVerificationCodeService
                        .issueFor(user)
        ).thenReturn(issuedCode);

        IssuedEmailVerificationCode result =
                service.issueAndSend(user);

        ArgumentCaptor<VerificationEmailMessage>
                messageCaptor =
                ArgumentCaptor.forClass(
                        VerificationEmailMessage.class
                );

        verify(verificationEmailSender)
                .send(messageCaptor.capture());

        VerificationEmailMessage sentMessage =
                messageCaptor.getValue();

        assertAll(
                () -> assertSame(
                        issuedCode,
                        result
                ),

                () -> assertEquals(
                        "Gabriel",
                        sentMessage.recipientName()
                ),

                () -> assertEquals(
                        "gabriel@example.com",
                        sentMessage.recipientEmail()
                ),

                () -> assertEquals(
                        "483921",
                        sentMessage.verificationCode()
                ),

                () -> assertEquals(
                        10,
                        sentMessage.expirationMinutes()
                )
        );
    }

    private static String validSecret() {
        byte[] bytes = new byte[32];

        return Base64
                .getEncoder()
                .encodeToString(bytes);
    }
}
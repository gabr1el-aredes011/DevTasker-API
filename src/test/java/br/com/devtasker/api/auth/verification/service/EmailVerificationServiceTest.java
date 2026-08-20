package br.com.devtasker.api.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;
import br.com.devtasker.api.auth.verification.domain.EmailVerificationCode;
import br.com.devtasker.api.auth.verification.repository.EmailVerificationCodeRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL =
            "gabriel@example.com";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailVerificationCodeRepository
            emailVerificationCodeRepository;

    @Mock
    private EmailVerificationCodeService
            emailVerificationCodeService;

    @Mock
    private EmailVerificationDeliveryService
            emailVerificationDeliveryService;

    @Mock
    private UserAccount user;

    @Mock
    private EmailVerificationCode verificationCode;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties =
                new EmailVerificationProperties(
                        6,
                        10,
                        3,
                        120,
                        validSecret()
                );

        service = new EmailVerificationService(
                userAccountRepository,
                emailVerificationCodeRepository,
                emailVerificationCodeService,
                emailVerificationDeliveryService,
                properties
        );
    }

    @Test
    void shouldRejectCodeThatDoesNotMatchConfiguredFormat() {
        EmailVerificationResult result = service.confirm(
                EMAIL,
                "12AB"
        );

        assertEquals(
                EmailVerificationResult.INVALID_CODE,
                result
        );

        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void shouldUseConfiguredMaximumAttempts() {
        prepareUnverifiedUser();

        when(verificationCode.isExpiredAt(any()))
                .thenReturn(false);

        when(verificationCode.hasReachedAttemptLimit(3))
                .thenReturn(true);

        EmailVerificationResult result = service.confirm(
                EMAIL,
                "483921"
        );

        assertEquals(
                EmailVerificationResult.ATTEMPTS_EXHAUSTED,
                result
        );

        verify(verificationCode)
                .hasReachedAttemptLimit(3);

        verify(emailVerificationCodeService, never())
                .matches(any(), any());
    }

    @Test
    void shouldUseConfiguredResendInterval() {
        prepareUnverifiedUser();

        when(verificationCode.canBeResentAt(any(), eq(120L)))
                .thenReturn(false);

        service.resend(EMAIL);

        verify(verificationCode)
                .canBeResentAt(any(), eq(120L));

        verify(emailVerificationDeliveryService, never())
                .issueAndSend(any(UserAccount.class));
    }

    private void prepareUnverifiedUser() {
        when(userAccountRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(user.isEmailVerified())
                .thenReturn(false);

        when(user.getId())
                .thenReturn(7L);

        when(emailVerificationCodeRepository.findByUserId(any()))
                .thenReturn(Optional.of(verificationCode));
    }

    private static String validSecret() {
        return Base64
                .getEncoder()
                .encodeToString(new byte[32]);
    }
}

package br.com.devtasker.api.auth.verification.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.auth.verification.config.EmailVerificationProperties;
import br.com.devtasker.api.auth.verification.domain.EmailVerificationCode;
import br.com.devtasker.api.auth.verification.repository.EmailVerificationCodeRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class EmailVerificationService {

    private final UserAccountRepository
            userAccountRepository;

    private final EmailVerificationCodeRepository
            emailVerificationCodeRepository;

    private final EmailVerificationCodeService
            emailVerificationCodeService;

    private final EmailVerificationDeliveryService
            emailVerificationDeliveryService;

    private final EmailVerificationProperties
            properties;

    public EmailVerificationService(
            UserAccountRepository userAccountRepository,
            EmailVerificationCodeRepository
                    emailVerificationCodeRepository,
            EmailVerificationCodeService
                    emailVerificationCodeService,
            EmailVerificationDeliveryService
                    emailVerificationDeliveryService,
            EmailVerificationProperties properties
    ) {
        this.userAccountRepository =
                userAccountRepository;

        this.emailVerificationCodeRepository =
                emailVerificationCodeRepository;

        this.emailVerificationCodeService =
                emailVerificationCodeService;

        this.emailVerificationDeliveryService =
                emailVerificationDeliveryService;

        this.properties = properties;
    }

    @Transactional
    public EmailVerificationResult confirm(
            String email,
            String rawCode
    ) {
        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String normalizedCode =
                rawCode.trim();

        if (!hasValidCodeFormat(normalizedCode)) {
            return EmailVerificationResult.INVALID_CODE;
        }

        UserAccount user =
                userAccountRepository
                        .findByEmail(normalizedEmail)
                        .orElse(null);

        /*
         * Evita revelar explicitamente
         * se determinado e-mail existe.
         */
        if (user == null) {
            return EmailVerificationResult.INVALID_CODE;
        }

        if (user.isEmailVerified()) {
            return EmailVerificationResult.ALREADY_VERIFIED;
        }

        EmailVerificationCode verificationCode =
                emailVerificationCodeRepository
                        .findByUserId(user.getId())
                        .orElse(null);

        if (verificationCode == null) {
            return EmailVerificationResult.INVALID_CODE;
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        /*
         * Um código expirado não pode mais
         * participar da autenticação.
         */
        if (verificationCode.isExpiredAt(now)) {
            return EmailVerificationResult.EXPIRED;
        }

        /*
         * Se o limite já foi atingido,
         * nem fazemos novamente o cálculo
         * criptográfico do código.
         */
        if (
                verificationCode.hasReachedAttemptLimit(
                        properties.maximumAttempts()
                )
        ) {
            return EmailVerificationResult.ATTEMPTS_EXHAUSTED;
        }

        boolean codeMatches =
                emailVerificationCodeService.matches(
                        verificationCode,
                        normalizedCode
                );

        if (!codeMatches) {

            /*
             * Registra a falha.
             */
            verificationCode.registerFailedAttempt();

            emailVerificationCodeRepository.save(
                    verificationCode
            );

            /*
             * A tentativa que acabou de falhar
             * pode ter sido justamente a última.
             */
            if (
                    verificationCode.hasReachedAttemptLimit(
                            properties.maximumAttempts()
                    )
            ) {
                return EmailVerificationResult.ATTEMPTS_EXHAUSTED;
            }

            return EmailVerificationResult.INVALID_CODE;
        }

        /*
         * Código correto.
         */
        user.verifyEmail();

        /*
         * Destrói a credencial temporária.
         */
        emailVerificationCodeRepository
                .deleteByUserId(user.getId());

        return EmailVerificationResult.VERIFIED;
    }
    
    @Transactional
    public void resend(
            String email
    ) {
        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase(Locale.ROOT);

        UserAccount user =
                userAccountRepository
                        .findByEmail(normalizedEmail)
                        .orElse(null);

        /*
         * Retornamos silenciosamente.
         *
         * Assim uma pessoa externa não consegue
         * descobrir facilmente quais e-mails
         * possuem conta no DevTasker.
         */
        if (user == null) {
            return;
        }

        /*
         * Uma conta já confirmada não precisa
         * receber outro código.
         */
        if (user.isEmailVerified()) {
            return;
        }

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        EmailVerificationCode existingCode =
                emailVerificationCodeRepository
                        .findByUserId(user.getId())
                        .orElse(null);

        /*
         * Caso exista um código recente,
         * aplicamos cooldown.
         */
        if (
                existingCode != null &&
                !existingCode.canBeResentAt(
                        now,
                        properties.resendIntervalSeconds()
                )
        ) {
            return;
        }

        /*
         * Cria um novo código e envia
         * utilizando exatamente a infraestrutura
         * de e-mail que já funciona.
         */
        emailVerificationDeliveryService
                .issueAndSend(user);
    }

    private boolean hasValidCodeFormat(
            String code
    ) {
        if (code.length() != properties.codeLength()) {
            return false;
        }

        return code
                .chars()
                .allMatch(Character::isDigit);
    }
}

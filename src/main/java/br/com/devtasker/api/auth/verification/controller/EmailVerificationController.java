package br.com.devtasker.api.auth.verification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.auth.verification.dto.ResendEmailVerificationRequest;
import br.com.devtasker.api.auth.verification.dto.VerifyEmailRequest;
import br.com.devtasker.api.auth.verification.service.EmailVerificationResult;
import br.com.devtasker.api.auth.verification.service.EmailVerificationService;
import br.com.devtasker.api.exception.EmailVerificationException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/email-verification")
public class EmailVerificationController {

    private final EmailVerificationService
            emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService
                    emailVerificationService
    ) {
        this.emailVerificationService =
                emailVerificationService;
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @Valid
            @RequestBody
            VerifyEmailRequest request
    ) {
        EmailVerificationResult result =
                emailVerificationService.confirm(
                        request.email(),
                        request.code()
                );

        return switch (result) {

            case VERIFIED,
                 ALREADY_VERIFIED ->
                    ResponseEntity
                            .noContent()
                            .build();

            case INVALID_CODE ->
                    throw EmailVerificationException
                            .invalidCode();

            case EXPIRED ->
                    throw EmailVerificationException
                            .expiredCode();

            case ATTEMPTS_EXHAUSTED ->
                    throw EmailVerificationException
                            .attemptsExhausted();
        };
    }
    
    @PostMapping("/resend")
    public ResponseEntity<Void> resend(
            @Valid
            @RequestBody
            ResendEmailVerificationRequest request
    ) {
        emailVerificationService.resend(
                request.email()
        );

        return ResponseEntity
                .accepted()
                .build();
    }
}

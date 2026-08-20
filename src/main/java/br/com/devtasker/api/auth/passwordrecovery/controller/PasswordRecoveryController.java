package br.com.devtasker.api.auth.passwordrecovery.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.auth.passwordrecovery.dto.PasswordRecoveryChallengeResponse;
import br.com.devtasker.api.auth.passwordrecovery.dto.RequestPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.ResendPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.ResetPasswordRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.VerifyPasswordRecoveryRequest;
import br.com.devtasker.api.auth.passwordrecovery.dto.VerifyPasswordRecoveryResponse;
import br.com.devtasker.api.auth.passwordrecovery.service.PasswordRecoveryFingerprintService;
import br.com.devtasker.api.auth.passwordrecovery.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/password-recovery")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;
    private final PasswordRecoveryFingerprintService fingerprintService;

    public PasswordRecoveryController(
            PasswordRecoveryService passwordRecoveryService,
            PasswordRecoveryFingerprintService fingerprintService
    ) {
        this.passwordRecoveryService = passwordRecoveryService;
        this.fingerprintService = fingerprintService;
    }

    @PostMapping("/request")
    public ResponseEntity<PasswordRecoveryChallengeResponse> request(
            @RequestBody RequestPasswordRecoveryRequest payload,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity
                .accepted()
                .cacheControl(CacheControl.noStore())
                .body(
                        passwordRecoveryService.request(
                                payload == null ? null : payload.email(),
                                fingerprintService.clientFingerprint(
                                        servletRequest
                                )
                        )
                );
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resend(
            @RequestBody ResendPasswordRecoveryRequest payload,
            HttpServletRequest servletRequest
    ) {
        passwordRecoveryService.resend(
                payload == null ? null : payload.challengeId(),
                fingerprintService.clientFingerprint(servletRequest)
        );
        return ResponseEntity
                .accepted()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPasswordRecoveryResponse> verify(
            @RequestBody VerifyPasswordRecoveryRequest payload,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        passwordRecoveryService.verify(
                                payload == null ? null : payload.challengeId(),
                                payload == null ? null : payload.code(),
                                fingerprintService.clientFingerprint(
                                        servletRequest
                                )
                        )
                );
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(
            @RequestBody ResetPasswordRequest payload,
            HttpServletRequest servletRequest
    ) {
        passwordRecoveryService.reset(
                payload == null ? null : payload.resetToken(),
                payload == null ? null : payload.newPassword(),
                fingerprintService.clientFingerprint(servletRequest)
        );

        return ResponseEntity
                .noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }
}

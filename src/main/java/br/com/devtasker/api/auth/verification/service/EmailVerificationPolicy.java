package br.com.devtasker.api.auth.verification.service;

public final class EmailVerificationPolicy {

    public static final int MAXIMUM_ATTEMPTS = 5;

    public static final long RESEND_COOLDOWN_SECONDS = 60L;

    private EmailVerificationPolicy() {
    }
}
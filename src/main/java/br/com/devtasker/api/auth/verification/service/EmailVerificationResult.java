package br.com.devtasker.api.auth.verification.service;

public enum EmailVerificationResult {

    VERIFIED,
    ALREADY_VERIFIED,
    INVALID_CODE,
    EXPIRED,
    ATTEMPTS_EXHAUSTED

}
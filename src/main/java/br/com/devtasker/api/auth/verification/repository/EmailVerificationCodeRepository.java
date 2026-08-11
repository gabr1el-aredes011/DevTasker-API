package br.com.devtasker.api.auth.verification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.auth.verification.domain.EmailVerificationCode;

public interface EmailVerificationCodeRepository
        extends JpaRepository<
                EmailVerificationCode,
                Long
        > {

    Optional<EmailVerificationCode>
            findByUserId(Long userId);

    Optional<EmailVerificationCode>
            findByUserEmail(String email);

    void deleteByUserId(Long userId);
}
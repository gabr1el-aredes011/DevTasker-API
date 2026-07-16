package br.com.devtasker.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.user.domain.UserAccount;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}

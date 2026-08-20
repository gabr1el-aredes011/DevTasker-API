package br.com.devtasker.api.auth.passwordrecovery.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.devtasker.api.user.domain.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Repository
public class PasswordRecoveryUserLockRepository {

    private final EntityManager entityManager;

    public PasswordRecoveryUserLockRepository(
            EntityManager entityManager
    ) {
        this.entityManager = entityManager;
    }

    public Optional<UserAccount> findByEmailForUpdate(String email) {
        return entityManager
                .createQuery(
                        """
                        select user
                        from UserAccount user
                        where user.email = :email
                        """,
                        UserAccount.class
                )
                .setParameter("email", email)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    public Optional<UserAccount> findByIdForUpdate(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                entityManager.find(
                        UserAccount.class,
                        userId,
                        LockModeType.PESSIMISTIC_WRITE
                )
        );
    }
}

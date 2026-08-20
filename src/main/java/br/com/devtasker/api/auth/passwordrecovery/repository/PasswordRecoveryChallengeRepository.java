package br.com.devtasker.api.auth.passwordrecovery.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.devtasker.api.auth.passwordrecovery.domain.PasswordRecoveryChallenge;
import jakarta.persistence.LockModeType;

public interface PasswordRecoveryChallengeRepository
        extends JpaRepository<PasswordRecoveryChallenge, Long> {

    @Query("""
            select challenge.user.id
            from PasswordRecoveryChallenge challenge
            where challenge.publicId = :publicId
            """)
    Optional<Long> findUserIdByPublicId(
            @Param("publicId") UUID publicId
    );

    @Query("""
            select challenge.user.id
            from PasswordRecoveryChallenge challenge
            where challenge.resetTokenHash = :tokenHash
            """)
    Optional<Long> findUserIdByResetTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from PasswordRecoveryChallenge challenge
            join fetch challenge.user
            where challenge.user.id = :userId
            """)
    Optional<PasswordRecoveryChallenge> findByUserIdForUpdate(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from PasswordRecoveryChallenge challenge
            join fetch challenge.user
            where challenge.publicId = :publicId
            """)
    Optional<PasswordRecoveryChallenge> findByPublicIdForUpdate(
            @Param("publicId") UUID publicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select challenge
            from PasswordRecoveryChallenge challenge
            join fetch challenge.user
            where challenge.resetTokenHash = :tokenHash
            """)
    Optional<PasswordRecoveryChallenge> findByResetTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}

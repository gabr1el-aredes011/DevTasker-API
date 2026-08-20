package br.com.devtasker.api.security.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class CredentialVersionJwtValidatorTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAccount user;

    private CredentialVersionJwtValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CredentialVersionJwtValidator(
                userAccountRepository
        );
    }

    @Test
    void shouldAcceptTokenWithCurrentCredentialVersion() {
        when(userAccountRepository.findById(7L))
                .thenReturn(Optional.of(user));

        when(user.getCredentialVersion())
                .thenReturn(3L);

        OAuth2TokenValidatorResult result = validator.validate(
                tokenWithClaims(7L, 3L)
        );

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldRejectTokenWithoutCredentialVersion() {
        Jwt token = Jwt.withTokenValue("signed-token")
                .header("alg", "HS256")
                .claim("user_id", 7L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        OAuth2TokenValidatorResult result =
                validator.validate(token);

        assertTrue(result.hasErrors());
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void shouldRejectTokenWithoutUserId() {
        Jwt token = Jwt.withTokenValue("signed-token")
                .header("alg", "HS256")
                .claim("credential_version", 3L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        OAuth2TokenValidatorResult result =
                validator.validate(token);

        assertTrue(result.hasErrors());
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void shouldRejectTokenWithInvalidClaimType() {
        OAuth2TokenValidatorResult result = validator.validate(
                tokenWithClaims(7L, "3")
        );

        assertTrue(result.hasErrors());
        verifyNoInteractions(userAccountRepository);
    }

    @Test
    void shouldRejectTokenForMissingUser() {
        when(userAccountRepository.findById(7L))
                .thenReturn(Optional.empty());

        OAuth2TokenValidatorResult result = validator.validate(
                tokenWithClaims(7L, 3L)
        );

        assertTrue(result.hasErrors());
    }

    @Test
    void shouldRejectTokenWithOutdatedCredentialVersion() {
        when(userAccountRepository.findById(7L))
                .thenReturn(Optional.of(user));

        when(user.getCredentialVersion())
                .thenReturn(4L);

        OAuth2TokenValidatorResult result = validator.validate(
                tokenWithClaims(7L, 3L)
        );

        assertTrue(result.hasErrors());
    }

    private static Jwt tokenWithClaims(
            Object userId,
            Object credentialVersion
    ) {
        Instant issuedAt = Instant.now();

        return Jwt.withTokenValue("signed-token")
                .header("alg", "HS256")
                .claim("user_id", userId)
                .claim(
                        "credential_version",
                        credentialVersion
                )
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(60))
                .build();
    }
}

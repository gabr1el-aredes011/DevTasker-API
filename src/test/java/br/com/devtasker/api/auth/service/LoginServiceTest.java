package br.com.devtasker.api.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import br.com.devtasker.api.auth.dto.LoginRequest;
import br.com.devtasker.api.auth.dto.LoginResponse;
import br.com.devtasker.api.exception.EmailNotVerifiedException;
import br.com.devtasker.api.security.jwt.AccessToken;
import br.com.devtasker.api.security.jwt.JwtTokenService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.domain.UserRole;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String NORMALIZED_EMAIL =
            "gabriel@example.com";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserAccount user;

    private LoginService service;

    @BeforeEach
    void setUp() {
        service = new LoginService(
                authenticationManager,
                userAccountRepository,
                jwtTokenService
        );
    }

    @Test
    void shouldBlockLoginUntilEmailIsVerified() {
        when(userAccountRepository.findByEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(user));

        when(user.isEmailVerified())
                .thenReturn(false);

        LoginRequest request = new LoginRequest(
                "  Gabriel@Example.com  ",
                "secret123"
        );

        assertThrows(
                EmailNotVerifiedException.class,
                () -> service.login(request)
        );

        verify(authenticationManager)
                .authenticate(any(Authentication.class));

        verify(jwtTokenService, never())
                .generate(any(UserAccount.class));
    }

    @Test
    void shouldIssueTokenForVerifiedUser() {
        Instant expiresAt = Instant.parse(
                "2026-08-20T15:00:00Z"
        );

        when(userAccountRepository.findByEmail(NORMALIZED_EMAIL))
                .thenReturn(Optional.of(user));

        when(user.isEmailVerified())
                .thenReturn(true);

        when(jwtTokenService.generate(user))
                .thenReturn(new AccessToken(
                        "signed-token",
                        expiresAt
                ));

        when(user.getId()).thenReturn(7L);
        when(user.getName()).thenReturn("Gabriel");
        when(user.getEmail()).thenReturn(NORMALIZED_EMAIL);
        when(user.getRole()).thenReturn(UserRole.USER);

        LoginResponse response = service.login(
                new LoginRequest(
                        NORMALIZED_EMAIL,
                        "secret123"
                )
        );

        assertAll(
                () -> assertEquals(
                        "signed-token",
                        response.accessToken()
                ),
                () -> assertEquals(
                        "Bearer",
                        response.tokenType()
                ),
                () -> assertEquals(
                        expiresAt,
                        response.expiresAt()
                ),
                () -> assertEquals(
                        7L,
                        response.userId()
                )
        );
    }
}

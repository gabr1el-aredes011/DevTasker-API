package br.com.devtasker.api.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.domain.UserRole;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class JwtCredentialVersionIntegrationTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAccount user;

    @Test
    void shouldIssueAndValidateCurrentCredentialVersion() {
        JwtConfig config = new JwtConfig();

        SecretKey secretKey = config.jwtSecretKey(
                validSecret()
        );

        JwtEncoder encoder = config.jwtEncoder(secretKey);

        CredentialVersionJwtValidator credentialValidator =
                new CredentialVersionJwtValidator(
                        userAccountRepository
                );

        JwtDecoder decoder = config.jwtDecoder(
                secretKey,
                "devtasker-api-test",
                credentialValidator
        );

        JwtTokenService tokenService = new JwtTokenService(
                encoder,
                "devtasker-api-test",
                30
        );

        prepareUser(4L);

        AccessToken accessToken = tokenService.generate(user);
        Jwt decodedToken = decoder.decode(accessToken.value());

        assertEquals(
                4L,
                ((Number) decodedToken.getClaim(
                        "credential_version"
                )).longValue()
        );

        when(user.getCredentialVersion())
                .thenReturn(5L);

        assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(accessToken.value())
        );
    }

    private void prepareUser(
            long credentialVersion
    ) {
        when(user.getId()).thenReturn(7L);
        when(user.getEmail())
                .thenReturn("gabriel@example.com");
        when(user.getName()).thenReturn("Gabriel");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getCredentialVersion())
                .thenReturn(credentialVersion);

        when(userAccountRepository.findById(7L))
                .thenReturn(Optional.of(user));
    }

    private static String validSecret() {
        return Base64
                .getEncoder()
                .encodeToString(new byte[32]);
    }
}

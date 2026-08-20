package br.com.devtasker.api.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import br.com.devtasker.api.user.repository.UserAccountRepository;

@Component
public class CredentialVersionJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String USER_ID_CLAIM =
            "user_id";

    private static final String CREDENTIAL_VERSION_CLAIM =
            "credential_version";

    private static final OAuth2Error INVALID_TOKEN =
            new OAuth2Error(
                    "invalid_token",
                    "O token de acesso não é mais válido.",
                    null
            );

    private final UserAccountRepository userAccountRepository;

    public CredentialVersionJwtValidator(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository =
                userAccountRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(
            Jwt token
    ) {
        Long userId = readIntegerClaim(
                token,
                USER_ID_CLAIM
        );

        Long tokenCredentialVersion = readIntegerClaim(
                token,
                CREDENTIAL_VERSION_CLAIM
        );

        if (
                userId == null ||
                userId <= 0 ||
                tokenCredentialVersion == null ||
                tokenCredentialVersion < 0
        ) {
            return failure();
        }

        return userAccountRepository
                .findById(userId)
                .filter(user ->
                        user.getCredentialVersion()
                                == tokenCredentialVersion
                )
                .map(user ->
                        OAuth2TokenValidatorResult.success()
                )
                .orElseGet(
                        CredentialVersionJwtValidator::failure
                );
    }

    private static Long readIntegerClaim(
            Jwt token,
            String claimName
    ) {
        if (token == null) {
            return null;
        }

        Object claim = token.getClaim(claimName);

        if (
                !(claim instanceof Byte) &&
                !(claim instanceof Short) &&
                !(claim instanceof Integer) &&
                !(claim instanceof Long)
        ) {
            return null;
        }

        return ((Number) claim).longValue();
    }

    private static OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(
                INVALID_TOKEN
        );
    }
}

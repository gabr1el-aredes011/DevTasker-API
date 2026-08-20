package br.com.devtasker.api.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import br.com.devtasker.api.user.domain.UserAccount;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration accessTokenDuration;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-minutes}")
            long accessTokenMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenDuration =
                Duration.ofMinutes(accessTokenMinutes);
    }

    public AccessToken generate(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessTokenDuration);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("user_id", user.getId())
                .claim("name", user.getName())
                .claim(
                        "credential_version",
                        user.getCredentialVersion()
                )
                .claim(
                        "roles",
                        List.of(user.getRole().name())
                )
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String tokenValue = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(header, claims)
                )
                .getTokenValue();

        return new AccessToken(tokenValue, expiresAt);
    }
}

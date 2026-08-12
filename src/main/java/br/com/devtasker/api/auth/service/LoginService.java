package br.com.devtasker.api.auth.service;

import java.util.Locale;	

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.auth.dto.LoginRequest;
import br.com.devtasker.api.auth.dto.LoginResponse;
import br.com.devtasker.api.exception.EmailNotVerifiedException;
import br.com.devtasker.api.security.jwt.AccessToken;
import br.com.devtasker.api.security.jwt.JwtTokenService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;


@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenService jwtTokenService;

    public LoginService(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            JwtTokenService jwtTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        var authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizedEmail,
                        request.password()
                );

        authenticationManager.authenticate(authenticationRequest);

        UserAccount user = userAccountRepository
                .findByEmail(normalizedEmail)
                .orElseThrow();

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        AccessToken token = jwtTokenService.generate(user);

        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
package br.com.devtasker.api.security.authentication;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        UserAccount user = userAccountRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado."
                        )
                );

        return User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
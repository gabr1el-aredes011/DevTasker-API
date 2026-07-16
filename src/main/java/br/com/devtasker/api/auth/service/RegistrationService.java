package br.com.devtasker.api.auth.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.auth.dto.RegisterRequest;
import br.com.devtasker.api.auth.dto.RegisterResponse;
import br.com.devtasker.api.exception.EmailAlreadyInUseException;
import br.com.devtasker.api.project.service.WorkspaceProvisioningService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class RegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceProvisioningService
            workspaceProvisioningService;

    public RegistrationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            WorkspaceProvisioningService workspaceProvisioningService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.workspaceProvisioningService =
                workspaceProvisioningService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedName = request.name().trim();

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException();
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        UserAccount user = UserAccount.create(
                normalizedName,
                normalizedEmail,
                passwordHash
        );

        UserAccount savedUser =
                userAccountRepository.save(user);

        workspaceProvisioningService
                .createInitialWorkspace(savedUser);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }
}
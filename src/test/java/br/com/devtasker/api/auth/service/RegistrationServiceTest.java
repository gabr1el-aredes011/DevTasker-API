package br.com.devtasker.api.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.devtasker.api.auth.dto.RegisterRequest;
import br.com.devtasker.api.auth.dto.RegisterResponse;
import br.com.devtasker.api.auth.verification.service.EmailVerificationDeliveryService;
import br.com.devtasker.api.auth.verification.service.IssuedEmailVerificationCode;
import br.com.devtasker.api.exception.EmailAlreadyInUseException;
import br.com.devtasker.api.project.service.WorkspaceProvisioningService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.domain.UserRole;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final String NORMALIZED_EMAIL =
            "gabriel@example.com";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationDeliveryService
            emailVerificationDeliveryService;

    @Mock
    private WorkspaceProvisioningService
            workspaceProvisioningService;

    @Mock
    private UserAccount savedUser;

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                userAccountRepository,
                passwordEncoder,
                emailVerificationDeliveryService,
                workspaceProvisioningService
        );
    }

    @Test
    void shouldCreateWorkspaceBeforeSendingVerificationEmail() {
        OffsetDateTime createdAt = OffsetDateTime.of(
                2026,
                8,
                20,
                12,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        OffsetDateTime expiresAt = createdAt.plusMinutes(10);

        when(userAccountRepository.existsByEmail(NORMALIZED_EMAIL))
                .thenReturn(false);

        when(passwordEncoder.encode("secret123"))
                .thenReturn("encoded-password");

        when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .thenReturn(savedUser);

        when(emailVerificationDeliveryService.issueAndSend(savedUser))
                .thenReturn(new IssuedEmailVerificationCode(
                        "483921",
                        expiresAt
                ));

        when(savedUser.getId()).thenReturn(7L);
        when(savedUser.getName()).thenReturn("Gabriel");
        when(savedUser.getEmail()).thenReturn(NORMALIZED_EMAIL);
        when(savedUser.getRole()).thenReturn(UserRole.USER);
        when(savedUser.getCreatedAt()).thenReturn(createdAt);

        RegisterResponse response = service.register(
                new RegisterRequest(
                        "  Gabriel  ",
                        "  Gabriel@Example.com  ",
                        "secret123"
                )
        );

        ArgumentCaptor<UserAccount> userCaptor =
                ArgumentCaptor.forClass(UserAccount.class);

        InOrder order = inOrder(
                userAccountRepository,
                workspaceProvisioningService,
                emailVerificationDeliveryService
        );

        order.verify(userAccountRepository)
                .saveAndFlush(userCaptor.capture());

        order.verify(workspaceProvisioningService)
                .createInitialWorkspace(savedUser);

        order.verify(emailVerificationDeliveryService)
                .issueAndSend(savedUser);

        UserAccount createdUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        "Gabriel",
                        createdUser.getName()
                ),
                () -> assertEquals(
                        NORMALIZED_EMAIL,
                        createdUser.getEmail()
                ),
                () -> assertEquals(
                        "encoded-password",
                        createdUser.getPasswordHash()
                ),
                () -> assertEquals(
                        7L,
                        response.id()
                ),
                () -> assertEquals(
                        expiresAt,
                        response.verificationExpiresAt()
                )
        );
    }

    @Test
    void shouldRejectDuplicatedEmailBeforeProvisioningWorkspace() {
        when(userAccountRepository.existsByEmail(NORMALIZED_EMAIL))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyInUseException.class,
                () -> service.register(
                        new RegisterRequest(
                                "Gabriel",
                                NORMALIZED_EMAIL,
                                "secret123"
                        )
                )
        );

        verify(workspaceProvisioningService, never())
                .createInitialWorkspace(any(UserAccount.class));

        verify(emailVerificationDeliveryService, never())
                .issueAndSend(any(UserAccount.class));
    }
}

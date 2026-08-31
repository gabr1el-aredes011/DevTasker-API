package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.email.model.ProjectInvitationEmailMessage;
import br.com.devtasker.api.email.service.ProjectInvitationEmailSender;
import br.com.devtasker.api.exception.ProjectMembershipException;
import br.com.devtasker.api.project.config.ProjectInvitationProperties;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectInvitation;
import br.com.devtasker.api.project.domain.ProjectInvitationStatus;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.AcceptProjectInvitationRequest;
import br.com.devtasker.api.project.dto.InviteProjectMemberRequest;
import br.com.devtasker.api.project.repository.ProjectInvitationRepository;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ProjectInvitationServiceTest {

    @Mock private ProjectInvitationRepository invitations;
    @Mock private ProjectMemberRepository members;
    @Mock private UserAccountRepository users;
    @Mock private ProjectAccessService access;
    @Mock private ProjectMemberManagementService management;
    @Mock private ProjectInvitationEmailSender emailSender;
    @Mock private ProjectMember actor;
    @Mock private Project project;
    @Mock private UserAccount inviter;
    @Mock private UserAccount recipient;
    @Mock private ProjectInvitation invitation;

    private ProjectInvitationService service;
    private ProjectInvitationTokenService tokens;

    @BeforeEach
    void setUp() {
        tokens = new ProjectInvitationTokenService();
        service = new ProjectInvitationService(
                invitations, members, users, access, management, tokens, emailSender,
                new ProjectInvitationProperties(72, "http://localhost:4200")
        );
    }

    @Test
    void shouldCreateSecureInvitationForVerifiedAccount() {
        when(access.requireManagementAccess(7L, 1L)).thenReturn(actor);
        when(actor.getProject()).thenReturn(project);
        when(actor.getUser()).thenReturn(inviter);
        when(project.getName()).thenReturn("DevTasker");
        when(inviter.getName()).thenReturn("Gabriel");
        when(users.findByEmail("bianca@example.com")).thenReturn(Optional.of(recipient));
        when(recipient.isEmailVerified()).thenReturn(true);
        when(recipient.getId()).thenReturn(2L);
        when(recipient.getName()).thenReturn("Bianca");
        when(recipient.getEmail()).thenReturn("bianca@example.com");
        when(members.existsActiveMembership(7L, 2L)).thenReturn(false);
        when(invitations.findByProjectEmailAndStatus(7L, "bianca@example.com", ProjectInvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitations.saveAndFlush(any(ProjectInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.invite(
                7L, 1L,
                new InviteProjectMemberRequest(" Bianca@Example.com ", ProjectMemberRole.MEMBER)
        );

        assertEquals("bianca@example.com", response.invitedEmail());
        verify(emailSender).send(any(ProjectInvitationEmailMessage.class));
    }

    @Test
    void shouldRejectInvitationForExistingMember() {
        when(access.requireManagementAccess(7L, 1L)).thenReturn(actor);
        when(users.findByEmail("bianca@example.com")).thenReturn(Optional.of(recipient));
        when(recipient.isEmailVerified()).thenReturn(true);
        when(recipient.getId()).thenReturn(2L);
        when(members.existsActiveMembership(7L, 2L)).thenReturn(true);

        assertThrows(
                ProjectMembershipException.class,
                () -> service.invite(7L, 1L, new InviteProjectMemberRequest("bianca@example.com", ProjectMemberRole.MEMBER))
        );
    }

    @Test
    void shouldRejectAcceptanceByAnotherEmail() {
        when(users.findById(2L)).thenReturn(Optional.of(recipient));
        when(recipient.getEmail()).thenReturn("other@example.com");
        when(invitations.findByTokenHashForUpdate(tokens.hash("valid-token"))).thenReturn(Optional.of(invitation));
        when(invitation.getStatus()).thenReturn(ProjectInvitationStatus.PENDING);
        when(invitation.isExpired(any(OffsetDateTime.class))).thenReturn(false);
        when(invitation.getInvitedEmail()).thenReturn("bianca@example.com");

        assertThrows(
                ProjectMembershipException.class,
                () -> service.accept(2L, new AcceptProjectInvitationRequest("valid-token"))
        );
    }
}

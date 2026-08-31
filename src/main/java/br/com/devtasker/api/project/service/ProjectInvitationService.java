package br.com.devtasker.api.project.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.email.model.ProjectInvitationEmailMessage;
import br.com.devtasker.api.email.service.ProjectInvitationEmailSender;
import br.com.devtasker.api.exception.ProjectMembershipException;
import br.com.devtasker.api.project.config.ProjectInvitationProperties;
import br.com.devtasker.api.project.domain.ProjectInvitation;
import br.com.devtasker.api.project.domain.ProjectInvitationStatus;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.AcceptProjectInvitationRequest;
import br.com.devtasker.api.project.dto.InviteProjectMemberRequest;
import br.com.devtasker.api.project.dto.ProjectInvitationAcceptanceResponse;
import br.com.devtasker.api.project.dto.ProjectInvitationSummaryResponse;
import br.com.devtasker.api.project.dto.ProjectMemberSummaryResponse;
import br.com.devtasker.api.project.repository.ProjectInvitationRepository;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class ProjectInvitationService {

    private final ProjectInvitationRepository invitationRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserAccountRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectMemberManagementService memberManagementService;
    private final ProjectInvitationTokenService tokenService;
    private final ProjectInvitationEmailSender emailSender;
    private final ProjectInvitationProperties properties;

    public ProjectInvitationService(
            ProjectInvitationRepository invitationRepository,
            ProjectMemberRepository memberRepository,
            UserAccountRepository userRepository,
            ProjectAccessService accessService,
            ProjectMemberManagementService memberManagementService,
            ProjectInvitationTokenService tokenService,
            ProjectInvitationEmailSender emailSender,
            ProjectInvitationProperties properties
    ) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.memberManagementService = memberManagementService;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Transactional
    public ProjectInvitationSummaryResponse invite(
            Long projectId,
            Long actorUserId,
            InviteProjectMemberRequest request
    ) {
        ProjectMember actor = accessService.requireManagementAccess(projectId, actorUserId);
        memberManagementService.requireAssignableRole(actor, request.role());

        String email = normalizeEmail(request.email());
        UserAccount recipient = userRepository.findByEmail(email)
                .filter(UserAccount::isEmailVerified)
                .orElseThrow(() -> problem(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "INVITEE_ACCOUNT_UNAVAILABLE",
                        "A pessoa convidada precisa possuir uma conta DevTasker verificada."
                ));

        if (memberRepository.existsActiveMembership(projectId, recipient.getId())) {
            throw problem(HttpStatus.CONFLICT, "PROJECT_MEMBER_ALREADY_EXISTS", "Esta pessoa já participa do projeto.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        invitationRepository.findByProjectEmailAndStatus(projectId, email, ProjectInvitationStatus.PENDING)
                .ifPresent(existing -> {
                    if (!existing.isExpired(now)) {
                        throw problem(HttpStatus.CONFLICT, "PROJECT_INVITATION_ALREADY_PENDING", "Já existe um convite pendente para este e-mail.");
                    }
                    existing.expire(now);
                    invitationRepository.save(existing);
                });

        String rawToken = tokenService.generate();
        ProjectInvitation invitation = ProjectInvitation.create(
                actor.getProject(),
                actor.getUser(),
                email,
                request.role(),
                tokenService.hash(rawToken),
                now.plus(properties.expiration())
        );
        ProjectInvitation saved;
        try {
            saved = invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw problem(
                    HttpStatus.CONFLICT,
                    "PROJECT_INVITATION_ALREADY_PENDING",
                    "Já existe um convite pendente para este e-mail."
            );
        }

        emailSender.send(new ProjectInvitationEmailMessage(
                recipient.getName(),
                recipient.getEmail(),
                actor.getUser().getName(),
                actor.getProject().getName(),
                roleLabel(request.role()),
                properties.frontendBaseUrl() + "/convites/aceitar#token=" + rawToken,
                properties.expirationHours()
        ));

        return toSummary(saved);
    }

    @Transactional
    public List<ProjectInvitationSummaryResponse> findPending(Long projectId, Long actorUserId) {
        ProjectMember actor = accessService.requireManagementAccess(projectId, actorUserId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return invitationRepository.findByProjectAndStatus(projectId, ProjectInvitationStatus.PENDING)
                .stream()
                .peek(invitation -> {
                    if (invitation.isExpired(now)) {
                        invitation.expire(now);
                    }
                })
                .filter(invitation -> invitation.getStatus() == ProjectInvitationStatus.PENDING)
                .filter(invitation -> actor.getRole() == ProjectMemberRole.OWNER
                        || invitation.getRole() != ProjectMemberRole.ADMIN)
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void revoke(Long projectId, Long invitationId, Long actorUserId) {
        ProjectMember actor = accessService.requireManagementAccess(projectId, actorUserId);
        ProjectInvitation invitation = invitationRepository.findByIdForUpdate(projectId, invitationId)
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "PROJECT_INVITATION_NOT_FOUND", "O convite não foi encontrado."));

        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw problem(HttpStatus.CONFLICT, "PROJECT_INVITATION_NOT_PENDING", "Este convite não está mais pendente.");
        }

        memberManagementService.requireAssignableRole(actor, invitation.getRole());

        invitation.revoke(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public ProjectInvitationAcceptanceResponse accept(
            Long userId,
            AcceptProjectInvitationRequest request
    ) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATED_USER_NOT_FOUND", "A conta autenticada não está disponível."));
        ProjectInvitation invitation = invitationRepository
                .findByTokenHashForUpdate(tokenService.hash(request.token().trim()))
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "PROJECT_INVITATION_NOT_FOUND", "O convite é inválido ou não está mais disponível."));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw problem(HttpStatus.CONFLICT, "PROJECT_INVITATION_NOT_PENDING", "Este convite já foi utilizado ou revogado.");
        }

        if (invitation.isExpired(now)) {
            invitation.expire(now);
            throw problem(HttpStatus.GONE, "PROJECT_INVITATION_EXPIRED", "Este convite expirou. Solicite um novo convite ao responsável pelo projeto.");
        }

        if (!invitation.getInvitedEmail().equals(normalizeEmail(user.getEmail()))) {
            throw problem(HttpStatus.FORBIDDEN, "PROJECT_INVITATION_EMAIL_MISMATCH", "Este convite foi enviado para outra conta.");
        }

        if (memberRepository.existsActiveMembership(invitation.getProject().getId(), userId)) {
            throw problem(HttpStatus.CONFLICT, "PROJECT_MEMBER_ALREADY_EXISTS", "Sua conta já participa deste projeto.");
        }

        ProjectMember membership = memberRepository.save(ProjectMember.create(
                invitation.getProject(),
                user,
                invitation.getRole()
        ));
        invitation.accept(now);

        return new ProjectInvitationAcceptanceResponse(
                invitation.getProject().getId(),
                invitation.getProject().getName(),
                toMemberSummary(membership, userId)
        );
    }

    private ProjectInvitationSummaryResponse toSummary(ProjectInvitation invitation) {
        return new ProjectInvitationSummaryResponse(
                invitation.getId(),
                invitation.getInvitedEmail(),
                invitation.getRole(),
                invitation.getInvitedBy().getName(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    private ProjectMemberSummaryResponse toMemberSummary(ProjectMember member, Long currentUserId) {
        var user = member.getUser();
        return new ProjectMemberSummaryResponse(
                member.getId(), user.getId(), user.getName(), user.getEmail(),
                user.getProfileImageUrl(), member.getRole(), member.getJoinedAt(),
                user.getId().equals(currentUserId)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String roleLabel(ProjectMemberRole role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case MEMBER -> "Membro";
            case VIEWER -> "Visualizador";
            case OWNER -> throw new IllegalArgumentException("OWNER não pode ser convidado.");
        };
    }

    private ProjectMembershipException problem(HttpStatus status, String code, String message) {
        return new ProjectMembershipException(status, code, message);
    }
}

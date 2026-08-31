package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.exception.ProjectMembershipException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.dto.UpdateProjectMemberRoleRequest;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@ExtendWith(MockitoExtension.class)
class ProjectMemberManagementServiceTest {

    @Mock private ProjectMemberRepository repository;
    @Mock private ProjectAccessService accessService;
    @Mock private ProjectMember actor;
    @Mock private ProjectMember target;
    @Mock private Project project;
    @Mock private UserAccount actorUser;
    @Mock private UserAccount targetUser;

    private ProjectMemberManagementService service;

    @BeforeEach
    void setUp() {
        service = new ProjectMemberManagementService(repository, accessService);
    }

    @Test
    void ownerShouldChangeMemberRole() {
        prepare(ProjectMemberRole.OWNER, ProjectMemberRole.MEMBER);
        when(repository.save(target)).thenReturn(target);
        when(target.getId()).thenReturn(12L);
        when(target.getJoinedAt()).thenReturn(null);
        when(targetUser.getName()).thenReturn("Bianca");
        when(targetUser.getEmail()).thenReturn("bianca@example.com");

        service.changeRole(7L, 12L, 1L, new UpdateProjectMemberRoleRequest(ProjectMemberRole.ADMIN));

        verify(target).changeRole(ProjectMemberRole.ADMIN);
        verify(repository).save(target);
    }

    @Test
    void adminShouldNotPromoteAnotherMemberToAdmin() {
        prepare(ProjectMemberRole.ADMIN, ProjectMemberRole.MEMBER);

        assertThrows(
                ProjectMembershipException.class,
                () -> service.changeRole(7L, 12L, 1L, new UpdateProjectMemberRoleRequest(ProjectMemberRole.ADMIN))
        );
        verify(target, never()).changeRole(ProjectMemberRole.ADMIN);
    }

    @Test
    void ownerMembershipShouldNeverBeRemoved() {
        when(accessService.requireManagementAccess(7L, 1L)).thenReturn(actor);
        when(repository.findById(12L)).thenReturn(Optional.of(target));
        when(target.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(7L);
        when(project.getArchivedAt()).thenReturn(null);
        when(target.getRole()).thenReturn(ProjectMemberRole.OWNER);

        assertThrows(ProjectMembershipException.class, () -> service.remove(7L, 12L, 1L));
        verify(repository, never()).delete(target);
    }

    private void prepare(ProjectMemberRole actorRole, ProjectMemberRole targetRole) {
        when(accessService.requireManagementAccess(7L, 1L)).thenReturn(actor);
        when(repository.findById(12L)).thenReturn(Optional.of(target));
        when(actor.getRole()).thenReturn(actorRole);
        when(actor.getUser()).thenReturn(actorUser);
        when(actorUser.getId()).thenReturn(1L);
        when(target.getRole()).thenReturn(targetRole);
        when(target.getUser()).thenReturn(targetUser);
        when(targetUser.getId()).thenReturn(2L);
        when(target.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(7L);
        when(project.getArchivedAt()).thenReturn(null);
    }
}

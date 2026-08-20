package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectMember membership;

    private ProjectAccessService service;

    @BeforeEach
    void setUp() {
        service = new ProjectAccessService(
                projectMemberRepository
        );
    }

    @Test
    void shouldHideProjectsWithoutMembership() {
        when(
                projectMemberRepository
                        .findByProject_IdAndUser_Id(
                                PROJECT_ID,
                                USER_ID
                        )
        ).thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> service.requireMembership(
                        PROJECT_ID,
                        USER_ID
                )
        );
    }

    @Test
    void shouldAllowMembersToWrite() {
        prepareMembership(ProjectMemberRole.MEMBER);

        ProjectMember result = service.requireWriteAccess(
                PROJECT_ID,
                USER_ID
        );

        assertSame(membership, result);
    }

    @Test
    void shouldBlockViewersFromWriting() {
        prepareMembership(ProjectMemberRole.VIEWER);

        assertThrows(
                ProjectPermissionDeniedException.class,
                () -> service.requireWriteAccess(
                        PROJECT_ID,
                        USER_ID
                )
        );
    }

    @Test
    void shouldAllowAdministratorsToManageProject() {
        prepareMembership(ProjectMemberRole.ADMIN);

        ProjectMember result = service.requireManagementAccess(
                PROJECT_ID,
                USER_ID
        );

        assertSame(membership, result);
    }

    @Test
    void shouldBlockMembersFromManagingProject() {
        prepareMembership(ProjectMemberRole.MEMBER);

        assertThrows(
                ProjectPermissionDeniedException.class,
                () -> service.requireManagementAccess(
                        PROJECT_ID,
                        USER_ID
                )
        );
    }

    @Test
    void shouldRequireOwnerForOwnershipOperations() {
        prepareMembership(ProjectMemberRole.ADMIN);

        assertThrows(
                ProjectPermissionDeniedException.class,
                () -> service.requireOwnership(
                        PROJECT_ID,
                        USER_ID
                )
        );
    }

    private void prepareMembership(
            ProjectMemberRole role
    ) {
        when(
                projectMemberRepository
                        .findByProject_IdAndUser_Id(
                                PROJECT_ID,
                                USER_ID
                        )
        ).thenReturn(Optional.of(membership));

        when(membership.getRole())
                .thenReturn(role);
    }
}

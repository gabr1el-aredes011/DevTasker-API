package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.user.domain.UserAccount;

@ExtendWith(MockitoExtension.class)
class ProjectMemberQueryServiceTest {

    private static final Long PROJECT_ID = 11L;
    private static final Long CURRENT_USER_ID = 7L;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private ProjectMember ownerMembership;

    @Mock
    private ProjectMember memberMembership;

    @Mock
    private UserAccount owner;

    @Mock
    private UserAccount member;

    private ProjectMemberQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProjectMemberQueryService(
                projectMemberRepository,
                projectAccessService
        );
    }

    @Test
    void shouldListMembersByRoleAndIdentifyCurrentUser() {
        OffsetDateTime joinedAt = OffsetDateTime.parse(
                "2026-08-20T12:00:00Z"
        );

        prepareMembership(
                memberMembership,
                member,
                20L,
                9L,
                "Bianca",
                "bianca@example.com",
                ProjectMemberRole.MEMBER,
                joinedAt
        );
        prepareMembership(
                ownerMembership,
                owner,
                10L,
                CURRENT_USER_ID,
                "Gabriel",
                "gabriel@example.com",
                ProjectMemberRole.OWNER,
                joinedAt
        );

        when(
                projectMemberRepository.findActiveMembersByProject(
                        PROJECT_ID
                )
        ).thenReturn(List.of(memberMembership, ownerMembership));

        var members = service.findMembers(
                PROJECT_ID,
                CURRENT_USER_ID
        );

        verify(projectAccessService).requireMembership(
                PROJECT_ID,
                CURRENT_USER_ID
        );
        assertEquals(2, members.size());
        assertEquals(ProjectMemberRole.OWNER, members.get(0).role());
        assertTrue(members.get(0).currentUser());
        assertEquals("gabriel@example.com", members.get(0).email());
        assertEquals(ProjectMemberRole.MEMBER, members.get(1).role());
        assertFalse(members.get(1).currentUser());
    }

    private void prepareMembership(
            ProjectMember membership,
            UserAccount user,
            Long membershipId,
            Long userId,
            String name,
            String email,
            ProjectMemberRole role,
            OffsetDateTime joinedAt
    ) {
        when(membership.getId()).thenReturn(membershipId);
        when(membership.getUser()).thenReturn(user);
        when(membership.getRole()).thenReturn(role);
        when(membership.getJoinedAt()).thenReturn(joinedAt);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        when(user.getProfileImageUrl()).thenReturn(null);
    }
}

package br.com.devtasker.api.project.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    private ProjectQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProjectQueryService(
                projectMemberRepository,
                projectAccessService
        );
    }

    @Test
    void shouldTrimSearchQuery() {
        when(
                projectMemberRepository
                        .searchActiveProjectsByUser(
                                USER_ID,
                                "API"
                        )
        ).thenReturn(List.of());

        assertTrue(
                service.findProjectsByUser(
                        USER_ID,
                        "  API  "
                ).isEmpty()
        );
    }

    @Test
    void shouldTreatBlankSearchAsAbsent() {
        when(
                projectMemberRepository
                        .findActiveProjectsByUser(
                                USER_ID
                        )
        ).thenReturn(List.of());

        service.findProjectsByUser(
                USER_ID,
                "   "
        );

        verify(projectMemberRepository)
                .findActiveProjectsByUser(
                        USER_ID
                );
    }
}

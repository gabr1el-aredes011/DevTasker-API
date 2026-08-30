package br.com.devtasker.api.project.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.devtasker.api.board.dto.CreateBoardRequest;
import br.com.devtasker.api.board.service.BoardQueryService;
import br.com.devtasker.api.board.service.BoardCommandService;
import br.com.devtasker.api.project.service.ProjectCommandService;
import br.com.devtasker.api.project.service.ProjectMemberQueryService;
import br.com.devtasker.api.project.service.ProjectQueryService;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 11L;

    @Mock
    private ProjectQueryService projectQueryService;

    @Mock
    private ProjectCommandService projectCommandService;

    @Mock
    private BoardQueryService boardQueryService;

    @Mock
    private BoardCommandService boardCommandService;

    @Mock
    private ProjectMemberQueryService projectMemberQueryService;

    @Mock
    private Jwt jwt;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(
                projectQueryService,
                projectCommandService,
                boardQueryService,
                boardCommandService,
                projectMemberQueryService
        );

        when(jwt.getClaim("user_id"))
                .thenReturn(USER_ID);
    }

    @Test
    void shouldForwardSearchQueryToService() {
        when(
                projectQueryService.findProjectsByUser(
                        USER_ID,
                        "backend"
                )
        ).thenReturn(List.of());

        controller.findMyProjects(
                "backend",
                jwt
        );

        verify(projectQueryService)
                .findProjectsByUser(
                        USER_ID,
                        "backend"
                );
    }

    @Test
    void shouldArchiveProjectAndReturnNoContent() {
        ResponseEntity<Void> response =
                controller.archive(
                        PROJECT_ID,
                        jwt
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(projectCommandService)
                .archive(
                        PROJECT_ID,
                        USER_ID
                );
    }

    @Test
    void shouldCreateBoardInsideProjectForAuthenticatedUser() {
        CreateBoardRequest request =
                new CreateBoardRequest("Roadmap");

        controller.createBoard(
                PROJECT_ID,
                request,
                jwt
        );

        verify(boardCommandService).create(
                PROJECT_ID,
                USER_ID,
                request
        );
    }

    @Test
    void shouldListProjectMembersForAuthenticatedUser() {
        when(
                projectMemberQueryService.findMembers(
                        PROJECT_ID,
                        USER_ID
                )
        ).thenReturn(List.of());

        controller.findProjectMembers(
                PROJECT_ID,
                jwt
        );

        verify(projectMemberQueryService).findMembers(
                PROJECT_ID,
                USER_ID
        );
    }
}

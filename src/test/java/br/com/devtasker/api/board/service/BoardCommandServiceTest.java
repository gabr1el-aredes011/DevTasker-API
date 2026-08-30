package br.com.devtasker.api.board.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.dto.CreateBoardRequest;
import br.com.devtasker.api.board.dto.UpdateBoardRequest;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNameAlreadyInUseException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.service.ProjectAccessService;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceTest {

    private static final Long PROJECT_ID = 11L;
    private static final Long BOARD_ID = 21L;
    private static final Long USER_ID = 7L;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private BoardWorkflowProvisioningService provisioningService;

    @Mock
    private Project project;

    @Mock
    private ProjectMember membership;

    private BoardCommandService service;

    @BeforeEach
    void setUp() {
        service = new BoardCommandService(
                boardRepository,
                projectAccessService,
                provisioningService
        );
    }

    @Test
    void shouldCreateBoardWithManagementAccess() {
        Board board = Board.create(project, "Roadmap");

        when(projectAccessService.requireManagementAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(PROJECT_ID);
        when(provisioningService.createBoard(project, "Roadmap"))
                .thenReturn(board);

        var response = service.create(
                PROJECT_ID,
                USER_ID,
                new CreateBoardRequest("  Roadmap  ")
        );

        assertEquals("Roadmap", response.name());
        verify(provisioningService).createBoard(project, "Roadmap");
    }

    @Test
    void shouldRejectDuplicateActiveNameBeforeProvisioning() {
        when(projectAccessService.requireManagementAccess(PROJECT_ID, USER_ID))
                .thenReturn(membership);
        when(membership.getProject()).thenReturn(project);
        when(boardRepository
                .existsByProject_IdAndArchivedAtIsNullAndNameIgnoreCase(
                        PROJECT_ID,
                        "Roadmap"
                )).thenReturn(true);

        assertThrows(
                BoardNameAlreadyInUseException.class,
                () -> service.create(
                        PROJECT_ID,
                        USER_ID,
                        new CreateBoardRequest("Roadmap")
                )
        );

        verify(provisioningService, never())
                .createBoard(project, "Roadmap");
    }

    @Test
    void shouldRenameAndArchiveActiveBoard() {
        Board board = Board.create(project, "Roadmap");

        when(project.getId()).thenReturn(PROJECT_ID);
        when(boardRepository.findActiveByIdForUpdate(BOARD_ID))
                .thenReturn(Optional.of(board));
        when(boardRepository.saveAndFlush(board))
                .thenReturn(board);

        var response = service.update(
                BOARD_ID,
                USER_ID,
                new UpdateBoardRequest("  Entregas  ")
        );

        assertEquals("Entregas", response.name());

        service.archive(BOARD_ID, USER_ID);

        assertTrue(board.isArchived());
        verify(projectAccessService, times(2))
                .requireManagementAccess(PROJECT_ID, USER_ID);
        verify(boardRepository, times(2))
                .saveAndFlush(board);
    }
}

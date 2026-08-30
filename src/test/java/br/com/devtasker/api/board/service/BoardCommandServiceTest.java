package br.com.devtasker.api.board.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import br.com.devtasker.api.project.repository.ProjectRepository;
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
    private ProjectRepository projectRepository;

    @Mock
    private Project project;

    private BoardCommandService service;

    @BeforeEach
    void setUp() {
        service = new BoardCommandService(
                boardRepository,
                projectAccessService,
                provisioningService,
                projectRepository
        );
    }

    @Test
    void shouldCreateAdditionalBoardWithManagementAccess() {
        Board currentDefault = Board.create(project, "Principal");
        currentDefault.markAsDefault();
        Board board = Board.create(project, "Roadmap");

        when(project.getId()).thenReturn(PROJECT_ID);
        when(projectRepository.findActiveByIdForUpdate(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(boardRepository
                .findByProject_IdAndDefaultBoardTrueAndArchivedAtIsNull(
                        PROJECT_ID
                )).thenReturn(Optional.of(currentDefault));
        when(provisioningService.createBoard(project, "Roadmap"))
                .thenReturn(board);

        var response = service.create(
                PROJECT_ID,
                USER_ID,
                new CreateBoardRequest("  Roadmap  ")
        );

        assertEquals("Roadmap", response.name());
        assertFalse(response.defaultBoard());
        verify(projectAccessService)
                .requireManagementAccess(PROJECT_ID, USER_ID);
        verify(provisioningService).createBoard(project, "Roadmap");
    }

    @Test
    void shouldMakeCreatedBoardDefaultWhenProjectHasNoActiveDefault() {
        Board board = Board.create(project, "Retomada");
        board.markAsDefault();

        when(project.getId()).thenReturn(PROJECT_ID);
        when(projectRepository.findActiveByIdForUpdate(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(boardRepository
                .findByProject_IdAndDefaultBoardTrueAndArchivedAtIsNull(
                        PROJECT_ID
                )).thenReturn(Optional.empty());
        when(provisioningService.createDefaultBoard(project, "Retomada"))
                .thenReturn(board);

        var response = service.create(
                PROJECT_ID,
                USER_ID,
                new CreateBoardRequest("Retomada")
        );

        assertTrue(response.defaultBoard());
        verify(provisioningService)
                .createDefaultBoard(project, "Retomada");
    }

    @Test
    void shouldRejectDuplicateActiveNameBeforeProvisioning() {
        when(projectRepository.findActiveByIdForUpdate(PROJECT_ID))
                .thenReturn(Optional.of(project));
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
        verify(provisioningService, never())
                .createDefaultBoard(project, "Roadmap");
    }

    @Test
    void shouldRenameActiveBoard() {
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
        verify(projectAccessService)
                .requireManagementAccess(PROJECT_ID, USER_ID);
    }

    @Test
    void shouldSetDefaultAndClearPreviousBoard() {
        Board currentDefault = Board.create(project, "Principal");
        currentDefault.markAsDefault();
        Board target = Board.create(project, "Roadmap");

        when(project.getId()).thenReturn(PROJECT_ID);
        when(boardRepository.findByIdAndArchivedAtIsNull(BOARD_ID))
                .thenReturn(Optional.of(target));
        when(projectRepository.findActiveByIdForUpdate(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(boardRepository.findActiveByIdForUpdate(BOARD_ID))
                .thenReturn(Optional.of(target));
        when(boardRepository
                .findByProject_IdAndDefaultBoardTrueAndArchivedAtIsNull(
                        PROJECT_ID
                )).thenReturn(Optional.of(currentDefault));
        when(boardRepository.saveAndFlush(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.setDefault(
                BOARD_ID,
                USER_ID
        );

        assertFalse(currentDefault.isDefaultBoard());
        assertTrue(target.isDefaultBoard());
        assertTrue(response.defaultBoard());
        verify(boardRepository, times(2))
                .saveAndFlush(any(Board.class));
    }

    @Test
    void shouldPromoteFallbackWhenArchivingDefaultBoard() {
        Board currentDefault = Board.create(project, "Principal");
        currentDefault.markAsDefault();
        Board fallback = Board.create(project, "Roadmap");

        when(project.getId()).thenReturn(PROJECT_ID);
        when(boardRepository.findByIdAndArchivedAtIsNull(BOARD_ID))
                .thenReturn(Optional.of(currentDefault));
        when(projectRepository.findActiveByIdForUpdate(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(boardRepository.findActiveByIdForUpdate(BOARD_ID))
                .thenReturn(Optional.of(currentDefault));
        when(boardRepository
                .findFirstByProject_IdAndArchivedAtIsNullOrderByIdAsc(
                        PROJECT_ID
                )).thenReturn(Optional.of(fallback));

        service.archive(BOARD_ID, USER_ID);

        assertTrue(currentDefault.isArchived());
        assertFalse(currentDefault.isDefaultBoard());
        assertTrue(fallback.isDefaultBoard());
        verify(boardRepository, times(2))
                .saveAndFlush(any(Board.class));
    }
}

package br.com.devtasker.api.board.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.domain.BoardColumnCategory;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.project.domain.Project;

@ExtendWith(MockitoExtension.class)
class BoardWorkflowProvisioningServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private Project project;

    private BoardWorkflowProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new BoardWorkflowProvisioningService(
                boardRepository,
                boardColumnRepository
        );

        when(boardRepository.saveAndFlush(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateBoardWithStandardWorkflow() {
        Board board = service.createBoard(
                project,
                "  Roadmap  "
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BoardColumn>> columnsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(boardColumnRepository).saveAll(
                columnsCaptor.capture()
        );

        List<BoardColumn> columns = columnsCaptor.getValue();

        assertEquals("Roadmap", board.getName());
        assertFalse(board.isDefaultBoard());
        assertEquals(5, columns.size());
        assertEquals(BoardColumnCategory.BACKLOG, columns.get(0).getCategory());
        assertEquals(BoardColumnCategory.DONE, columns.get(4).getCategory());
        assertEquals(4, columns.get(4).getPosition());
    }

    @Test
    void shouldCreateDefaultBoardWithStandardWorkflow() {
        Board board = service.createDefaultBoard(
                project,
                "Principal"
        );

        assertTrue(board.isDefaultBoard());
        verify(boardColumnRepository).saveAll(any());
    }
}

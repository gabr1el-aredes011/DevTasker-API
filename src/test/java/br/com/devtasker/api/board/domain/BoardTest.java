package br.com.devtasker.api.board.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.devtasker.api.project.domain.Project;

class BoardTest {

    private final Project project = Mockito.mock(Project.class);

    @Test
    void shouldNormalizeNameOnCreateAndUpdate() {
        Board board = Board.create(project, "  Produto  ");

        board.updateName("  Entregas  ");

        assertEquals("Entregas", board.getName());
    }

    @Test
    void shouldRejectInvalidNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Board.create(project, "   ")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Board.create(project, "x".repeat(121))
        );
    }

    @Test
    void shouldArchiveOnlyOnce() {
        Board board = Board.create(project, "Produto");
        board.markAsDefault();

        board.archive();

        assertTrue(board.isArchived());
        assertFalse(board.isDefaultBoard());
        assertThrows(IllegalStateException.class, board::archive);
        assertThrows(
                IllegalStateException.class,
                () -> board.updateName("Novo nome")
        );
    }

    @Test
    void shouldCreateInitialBoardAsDefault() {
        Board board = Board.createInitial(project);

        assertTrue(board.isDefaultBoard());
    }
}

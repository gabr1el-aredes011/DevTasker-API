package br.com.devtasker.api.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.user.domain.UserAccount;

class TaskTest {

    @Test
    void shouldNormalizeAndDeduplicateLabelsWithoutChangingTheirOrder() {
        Task task = newTask();

        task.replaceLabels(List.of(" Backend ", "URGENTE", "backend"));

        assertEquals(List.of("Backend", "URGENTE"), task.getLabels());
    }

    @Test
    void shouldRejectMoreThanFiveDistinctLabels() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.replaceLabels(
                        List.of("A", "B", "C", "D", "E", "F")
                )
        );
    }

    @Test
    void shouldRejectBlankLabels() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.replaceLabels(List.of("Backend", " "))
        );
    }

    @Test
    void shouldManageChecklistItemsInsideTaskAggregate() {
        Task task = newTask();

        TaskChecklistItem first = task.addChecklistItem("  Preparar cenário  ");
        TaskChecklistItem second = task.addChecklistItem("Executar teste");

        assertEquals("Preparar cenário", first.getTitle());
        assertFalse(first.isCompleted());
        assertEquals(0, first.getPosition());
        assertEquals(1, second.getPosition());

        first.update(first.getTitle(), true);
        assertTrue(first.isCompleted());

        task.removeChecklistItem(second);
        assertEquals(List.of(first), task.getChecklistItems());
    }

    @Test
    void shouldRejectBlankChecklistItemTitle() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.addChecklistItem(" ")
        );
    }

    @Test
    void shouldPreserveMarkdownAndRejectOversizedDescription() {
        String markdown = "# Objetivo\n\n- [ ] Validar entrega\n- **Documentar** decisão";
        Task task = Task.create(
                Mockito.mock(BoardColumn.class),
                Mockito.mock(UserAccount.class),
                "Tarefa com contexto",
                markdown,
                TaskPriority.MEDIUM,
                null,
                0
        );

        assertEquals(markdown, task.getDescription());
        assertThrows(
                IllegalArgumentException.class,
                () -> task.updateDetails(
                        "Tarefa com contexto",
                        "a".repeat(Task.MAXIMUM_DESCRIPTION_LENGTH + 1),
                        TaskPriority.MEDIUM,
                        null
                )
        );
    }

    private Task newTask() {
        return Task.create(
                Mockito.mock(BoardColumn.class),
                Mockito.mock(UserAccount.class),
                "Tarefa",
                null,
                TaskPriority.MEDIUM,
                null,
                0
        );
    }
}

package br.com.devtasker.api.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

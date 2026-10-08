package br.com.devtasker.api.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.user.domain.UserAccount;

class TaskTest {

    @Test
    void shouldDeduplicateCatalogLabelsWithoutChangingTheirOrder() {
        Task task = newTask();
        ProjectLabel backend = Mockito.mock(ProjectLabel.class);
        ProjectLabel urgent = Mockito.mock(ProjectLabel.class);

        task.replaceLabels(List.of(backend, urgent, backend));

        assertEquals(List.of(backend, urgent), task.getLabels());
    }

    @Test
    void shouldRejectMoreThanFiveDistinctLabels() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.replaceLabels(
                        List.of(
                                Mockito.mock(ProjectLabel.class),
                                Mockito.mock(ProjectLabel.class),
                                Mockito.mock(ProjectLabel.class),
                                Mockito.mock(ProjectLabel.class),
                                Mockito.mock(ProjectLabel.class),
                                Mockito.mock(ProjectLabel.class)
                        )
                )
        );
    }

    @Test
    void shouldRejectNullLabels() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.replaceLabels(java.util.Arrays.asList(Mockito.mock(ProjectLabel.class), null))
        );
    }

    @Test
    void shouldKeepTechnologiesUniqueAndInTheSelectedOrder() {
        Task task = newTask();

        task.replaceTechnologies(List.of(
                TaskTechnology.JAVA,
                TaskTechnology.ANGULAR,
                TaskTechnology.JAVA
        ));

        assertEquals(
                List.of(TaskTechnology.JAVA, TaskTechnology.ANGULAR),
                task.getTechnologies()
        );
    }

    @Test
    void shouldRejectMoreThanEightTechnologies() {
        Task task = newTask();

        assertThrows(
                IllegalArgumentException.class,
                () -> task.replaceTechnologies(List.of(
                        TaskTechnology.ANGULAR,
                        TaskTechnology.REACT,
                        TaskTechnology.VUE,
                        TaskTechnology.TYPESCRIPT,
                        TaskTechnology.JAVASCRIPT,
                        TaskTechnology.JAVA,
                        TaskTechnology.SPRING,
                        TaskTechnology.PYTHON,
                        TaskTechnology.DOCKER
                ))
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

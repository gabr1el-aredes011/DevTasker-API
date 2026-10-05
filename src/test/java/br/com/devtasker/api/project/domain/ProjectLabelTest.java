package br.com.devtasker.api.project.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProjectLabelTest {

    @Test
    void shouldNormalizeNameAndAllowChangingTheVisualIdentity() {
        ProjectLabel label = ProjectLabel.create(
                Mockito.mock(Project.class),
                "  BackEnd  ",
                ProjectLabelColor.BLUE
        );

        assertEquals("BackEnd", label.getName());
        assertEquals("backend", label.getNormalizedName());

        label.update("API", ProjectLabelColor.VIOLET);

        assertEquals("API", label.getName());
        assertEquals("api", label.getNormalizedName());
        assertEquals(ProjectLabelColor.VIOLET, label.getColor());
    }

    @Test
    void shouldArchiveWithoutErasingTheLabelIdentity() {
        ProjectLabel label = ProjectLabel.create(
                Mockito.mock(Project.class),
                "Urgente",
                ProjectLabelColor.RED
        );

        label.archive();

        assertTrue(label.isArchived());
        assertEquals("Urgente", label.getName());
    }

    @Test
    void shouldRejectInvalidCatalogData() {
        Project project = Mockito.mock(Project.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectLabel.create(project, " ", ProjectLabelColor.GREEN)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectLabel.create(project, "A".repeat(31), ProjectLabelColor.GREEN)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectLabel.create(project, "Backend", null)
        );
    }
}

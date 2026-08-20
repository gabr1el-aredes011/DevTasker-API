package br.com.devtasker.api.project.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import br.com.devtasker.api.user.domain.UserAccount;

class ProjectTest {

    private final UserAccount owner =
            Mockito.mock(UserAccount.class);

    @Test
    void shouldNormalizeProjectDetails() {
        Project project = Project.create(
                "  DevTasker API  ",
                "  Gestão do backend  ",
                owner
        );

        assertAll(
                () -> assertEquals(
                        "DevTasker API",
                        project.getName()
                ),
                () -> assertEquals(
                        "Gestão do backend",
                        project.getDescription()
                ),
                () -> assertSame(
                        owner,
                        project.getOwner()
                )
        );
    }

    @Test
    void shouldNormalizeBlankDescriptionToNull() {
        Project project = Project.create(
                "DevTasker",
                "   ",
                owner
        );

        assertNull(project.getDescription());
    }

    @Test
    void shouldRejectInvalidProjectData() {
        String nameTooLong = "a".repeat(121);
        String descriptionTooLong = "a".repeat(1001);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> Project.create(
                                "   ",
                                null,
                                owner
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> Project.create(
                                nameTooLong,
                                null,
                                owner
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> Project.create(
                                "DevTasker",
                                descriptionTooLong,
                                owner
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> Project.create(
                                "DevTasker",
                                null,
                                null
                        )
                )
        );
    }

    @Test
    void shouldUpdateAndNormalizeProjectDetails() {
        Project project = Project.create(
                "DevTasker",
                null,
                owner
        );

        project.updateDetails(
                "  DevTasker Web  ",
                "  Interface Angular  "
        );

        assertAll(
                () -> assertEquals(
                        "DevTasker Web",
                        project.getName()
                ),
                () -> assertEquals(
                        "Interface Angular",
                        project.getDescription()
                )
        );
    }
}

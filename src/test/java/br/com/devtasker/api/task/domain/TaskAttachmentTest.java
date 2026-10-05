package br.com.devtasker.api.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.user.domain.UserAccount;

class TaskAttachmentTest {

    @Test
    void shouldCreateAndSoftDeleteAttachment() {
        Task task = mock(Task.class);
        UserAccount uploader = mock(UserAccount.class);

        TaskAttachment attachment = TaskAttachment.create(
                task,
                uploader,
                " contrato.pdf ",
                "storage-key",
                "application/pdf",
                1024L
        );

        assertEquals("contrato.pdf", attachment.getOriginalFileName());
        assertEquals(task, attachment.getTask());
        assertEquals(uploader, attachment.getUploader());

        attachment.remove();

        assertNotNull(attachment.getDeletedAt());
    }

    @Test
    void shouldRejectInvalidAttachmentMetadata() {
        Task task = mock(Task.class);
        UserAccount uploader = mock(UserAccount.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> TaskAttachment.create(
                        task,
                        uploader,
                        " ",
                        "storage-key",
                        "application/pdf",
                        1024L
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TaskAttachment.create(
                        task,
                        uploader,
                        "contrato.pdf",
                        "storage-key",
                        "application/pdf",
                        0L
                )
        );
    }
}

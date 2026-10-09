package br.com.devtasker.api.task.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import br.com.devtasker.api.user.domain.UserAccount;

class TaskCommentTest {

    @Test
    void shouldCreateEditAndSoftDeleteComment() {
        Task task = mock(Task.class);
        UserAccount author = mock(UserAccount.class);

        TaskComment comment = TaskComment.create(task, author, "  Contexto inicial.  ");

        assertEquals("Contexto inicial.", comment.getContent());
        assertEquals(task, comment.getTask());
        assertEquals(author, comment.getAuthor());

        comment.edit("  Contexto atualizado.  ");

        assertEquals("Contexto atualizado.", comment.getContent());
        assertNotNull(comment.getEditedAt());

        comment.remove();

        assertNotNull(comment.getDeletedAt());
    }

    @Test
    void shouldRejectBlankOrOversizedContent() {
        Task task = mock(Task.class);
        UserAccount author = mock(UserAccount.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> TaskComment.create(task, author, "   ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TaskComment.create(
                        task,
                        author,
                        "a".repeat(TaskComment.MAXIMUM_CONTENT_LENGTH + 1)
                )
        );
    }

    @Test
    void shouldKeepRepliesInASingleReadableThread() {
        Task task = mock(Task.class);
        UserAccount firstAuthor = mock(UserAccount.class);
        UserAccount secondAuthor = mock(UserAccount.class);
        TaskComment root = TaskComment.create(task, firstAuthor, "Contexto inicial.");
        TaskComment reply = TaskComment.replyTo(task, secondAuthor, root, "Primeira resposta.");
        TaskComment nestedReply = TaskComment.replyTo(task, firstAuthor, reply, "Nova resposta.");

        assertSame(root, reply.getParentComment());
        assertSame(root, nestedReply.getParentComment());
        assertSame(root, nestedReply.root());
    }
}

package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.service.PasswordRecoveryEmailSender;

class PasswordRecoveryEmailDispatcherTest {

    @Test
    void shouldOnlySubmitEmailAfterCommit() {
        PasswordRecoveryEmailSender sender =
                mock(PasswordRecoveryEmailSender.class);
        Executor directExecutor = Runnable::run;
        PasswordRecoveryEmailDispatcher dispatcher =
                new PasswordRecoveryEmailDispatcher(
                        sender,
                        directExecutor
                );

        TransactionSynchronizationManager.initSynchronization();

        try {
            dispatcher.dispatchAfterCommit(message());
            verify(sender, never()).send(any());

            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager
                            .getSynchronizations()
                            .getFirst();
            synchronization.afterCommit();

            verify(sender).send(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldAbsorbExecutorRejectionEvenFromAfterCommitCallback() {
        PasswordRecoveryEmailSender sender =
                mock(PasswordRecoveryEmailSender.class);
        Executor executor = mock(Executor.class);
        doThrow(new RejectedExecutionException("queue full"))
                .when(executor)
                .execute(any(Runnable.class));
        PasswordRecoveryEmailDispatcher dispatcher =
                new PasswordRecoveryEmailDispatcher(sender, executor);

        TransactionSynchronizationManager.initSynchronization();

        try {
            dispatcher.dispatchAfterCommit(message());
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager
                            .getSynchronizations()
                            .getFirst();

            assertDoesNotThrow(synchronization::afterCommit);
            verify(sender, never()).send(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldAbsorbUnexpectedSenderFailure() {
        PasswordRecoveryEmailSender sender =
                mock(PasswordRecoveryEmailSender.class);
        doThrow(new IllegalStateException("unexpected"))
                .when(sender)
                .send(any());
        PasswordRecoveryEmailDispatcher dispatcher =
                new PasswordRecoveryEmailDispatcher(sender, Runnable::run);

        assertDoesNotThrow(
                () -> dispatcher.dispatchAfterCommit(message())
        );
    }

    private static PasswordRecoveryEmailMessage message() {
        return new PasswordRecoveryEmailMessage(
                "Dev User",
                "user@devtasker.test",
                "123456",
                10
        );
    }
}

package br.com.devtasker.api.auth.passwordrecovery.service;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;
import br.com.devtasker.api.email.service.PasswordRecoveryEmailSender;

@Component
public class PasswordRecoveryEmailDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            PasswordRecoveryEmailDispatcher.class
    );

    private final PasswordRecoveryEmailSender emailSender;
    private final Executor executor;

    public PasswordRecoveryEmailDispatcher(
            PasswordRecoveryEmailSender emailSender,
            @Qualifier("passwordRecoveryEmailExecutor") Executor executor
    ) {
        this.emailSender = emailSender;
        this.executor = executor;
    }

    public void dispatchAfterCommit(
            PasswordRecoveryEmailMessage message
    ) {
        Runnable submit = () -> submitSafely(message);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            submit.run();
                        }
                    }
            );
            return;
        }

        submit.run();
    }

    private void submitSafely(PasswordRecoveryEmailMessage message) {
        try {
            executor.execute(() -> sendSafely(message));
        } catch (RuntimeException exception) {
            logNeutralFailure(exception);
        }
    }

    private void sendSafely(PasswordRecoveryEmailMessage message) {
        try {
            emailSender.send(message);
        } catch (RuntimeException exception) {
            logNeutralFailure(exception);
        }
    }

    private static void logNeutralFailure(RuntimeException exception) {
        LOGGER.warn(
                "Falha assíncrona no e-mail de recuperação ({})",
                exception.getClass().getSimpleName()
        );
    }
}

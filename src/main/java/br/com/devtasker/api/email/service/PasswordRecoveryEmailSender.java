package br.com.devtasker.api.email.service;

import br.com.devtasker.api.email.model.PasswordRecoveryEmailMessage;

public interface PasswordRecoveryEmailSender {

    void send(PasswordRecoveryEmailMessage message);
}

package br.com.devtasker.api.email.service;

import br.com.devtasker.api.email.model.VerificationEmailMessage;

public interface VerificationEmailSender {

    void send(
            VerificationEmailMessage message
    );
}
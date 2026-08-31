package br.com.devtasker.api.email.service;

import br.com.devtasker.api.email.model.ProjectInvitationEmailMessage;

public interface ProjectInvitationEmailSender {
    void send(ProjectInvitationEmailMessage message);
}

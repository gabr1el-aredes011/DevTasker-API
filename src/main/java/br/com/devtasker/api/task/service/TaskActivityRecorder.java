package br.com.devtasker.api.task.service;

import org.springframework.stereotype.Service;

import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskActivity;
import br.com.devtasker.api.task.domain.TaskActivityType;
import br.com.devtasker.api.task.repository.TaskActivityRepository;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;

@Service
public class TaskActivityRecorder {

    private final TaskActivityRepository activityRepository;
    private final UserAccountRepository userAccountRepository;

    public TaskActivityRecorder(
            TaskActivityRepository activityRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.activityRepository = activityRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public void record(
            Task task,
            Long actorId,
            TaskActivityType type,
            String description
    ) {
        UserAccount actor = userAccountRepository.getReferenceById(actorId);
        record(task, actor, type, description);
    }

    public void record(
            Task task,
            UserAccount actor,
            TaskActivityType type,
            String description
    ) {
        activityRepository.save(TaskActivity.create(task, actor, type, description));
    }
}

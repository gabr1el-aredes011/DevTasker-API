CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    CONSTRAINT pk_task_assignees
        PRIMARY KEY (task_id, user_id),

    CONSTRAINT fk_task_assignees_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_assignees_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

INSERT INTO task_assignees (task_id, user_id)
SELECT id, assignee_id
FROM tasks
WHERE assignee_id IS NOT NULL;

CREATE INDEX idx_task_assignees_user_id
    ON task_assignees (user_id);

ALTER TABLE tasks
    DROP CONSTRAINT fk_tasks_assignee;

DROP INDEX idx_tasks_assignee_id;

ALTER TABLE tasks
    DROP COLUMN assignee_id;

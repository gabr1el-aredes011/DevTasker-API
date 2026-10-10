CREATE TABLE task_assignees (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    position INTEGER NOT NULL,

    CONSTRAINT pk_task_assignees
        PRIMARY KEY (task_id, user_id),

    CONSTRAINT uk_task_assignees_position
        UNIQUE (task_id, position),

    CONSTRAINT fk_task_assignees_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_assignees_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

INSERT INTO task_assignees (task_id, user_id, position)
SELECT id, assignee_id, 0
FROM tasks
WHERE assignee_id IS NOT NULL;

CREATE INDEX idx_task_assignees_user_id
    ON task_assignees (user_id);

ALTER TABLE tasks
    DROP CONSTRAINT fk_tasks_assignee;

DROP INDEX idx_tasks_assignee_id;

ALTER TABLE tasks
    DROP COLUMN assignee_id;

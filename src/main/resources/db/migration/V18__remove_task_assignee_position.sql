ALTER TABLE task_assignees
    DROP CONSTRAINT uk_task_assignees_position;

ALTER TABLE task_assignees
    DROP COLUMN position;

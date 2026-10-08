CREATE TABLE task_technologies (
    task_id BIGINT NOT NULL,
    technology VARCHAR(32) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT pk_task_technologies PRIMARY KEY (task_id, technology),
    CONSTRAINT fk_task_technologies_task
        FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT uq_task_technologies_position UNIQUE (task_id, position),
    CONSTRAINT ck_task_technologies_position CHECK (position >= 0)
);

CREATE INDEX idx_task_technologies_task_id ON task_technologies (task_id);

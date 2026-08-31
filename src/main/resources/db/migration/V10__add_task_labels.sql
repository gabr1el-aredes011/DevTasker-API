CREATE TABLE task_labels (
    task_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    label VARCHAR(30) NOT NULL,

    CONSTRAINT pk_task_labels
        PRIMARY KEY (task_id, position),

    CONSTRAINT fk_task_labels_task
        FOREIGN KEY (task_id)
        REFERENCES tasks (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_task_labels_position
        CHECK (position >= 0)
);

CREATE INDEX idx_task_labels_label
    ON task_labels (label);

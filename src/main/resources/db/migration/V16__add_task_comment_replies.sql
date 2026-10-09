ALTER TABLE task_comments
    ADD COLUMN parent_comment_id BIGINT;

ALTER TABLE task_comments
    ADD CONSTRAINT fk_task_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES task_comments(id) ON DELETE CASCADE;

CREATE INDEX idx_task_comments_parent_created
    ON task_comments(parent_comment_id, created_at);

ALTER TABLE boards
    ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE boards
    DROP CONSTRAINT uk_boards_project_name;

CREATE UNIQUE INDEX uk_boards_active_project_name
    ON boards (project_id, LOWER(name))
    WHERE archived_at IS NULL;

CREATE INDEX idx_boards_active_project_id
    ON boards (project_id, id)
    WHERE archived_at IS NULL;

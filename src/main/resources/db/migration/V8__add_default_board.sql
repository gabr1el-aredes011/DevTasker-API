ALTER TABLE boards
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

WITH first_active_board AS (
    SELECT DISTINCT ON (project_id)
        id
    FROM boards
    WHERE archived_at IS NULL
    ORDER BY project_id, id
)
UPDATE boards
SET is_default = TRUE
WHERE id IN (
    SELECT id
    FROM first_active_board
);

CREATE UNIQUE INDEX uk_boards_default_project
    ON boards (project_id)
    WHERE is_default = TRUE
      AND archived_at IS NULL;

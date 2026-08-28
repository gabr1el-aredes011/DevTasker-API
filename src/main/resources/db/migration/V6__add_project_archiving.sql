ALTER TABLE projects
    ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_projects_active_updated_at
    ON projects (updated_at DESC, id DESC)
    WHERE archived_at IS NULL;

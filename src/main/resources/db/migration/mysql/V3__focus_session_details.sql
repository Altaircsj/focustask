ALTER TABLE focus_sessions ADD COLUMN title VARCHAR(255);
ALTER TABLE focus_sessions ADD COLUMN planned_minutes INTEGER;
ALTER TABLE focus_sessions ADD COLUMN project_id BIGINT;
ALTER TABLE focus_sessions ADD CONSTRAINT fk_focus_sessions_project FOREIGN KEY (project_id) REFERENCES projects (id);
ALTER TABLE focus_sessions ADD CONSTRAINT ck_focus_sessions_link CHECK (task_id IS NULL OR project_id IS NULL);
ALTER TABLE focus_sessions ADD CONSTRAINT ck_focus_sessions_plan CHECK (planned_minutes BETWEEN 1 AND 180);
CREATE INDEX idx_focus_sessions_project ON focus_sessions (project_id);

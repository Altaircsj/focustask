ALTER TABLE projects ADD COLUMN color VARCHAR(7) NOT NULL DEFAULT '#4D49FC';
CREATE TABLE project_tags (
    project_id BIGINT NOT NULL,
    tag_position INTEGER NOT NULL,
    tag_text VARCHAR(30) NOT NULL,
    PRIMARY KEY (project_id, tag_position),
    CONSTRAINT fk_project_tags_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

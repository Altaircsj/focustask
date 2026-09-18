CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    PRIMARY KEY (id),
    INDEX idx_projects_user (user_id),
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE NULL,
    PRIMARY KEY (id),
    INDEX idx_tasks_project (project_id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE focus_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    paused_at DATETIME(6) NULL,
    total_paused_seconds BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_focus_sessions_user (user_id),
    INDEX idx_focus_sessions_task (task_id),
    CONSTRAINT fk_focus_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_focus_sessions_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE RESTRICT,
    CONSTRAINT ck_focus_sessions_state CHECK (
        (status = 'RUNNING' AND ended_at IS NULL AND paused_at IS NULL) OR
        (status = 'PAUSED' AND ended_at IS NULL AND paused_at IS NOT NULL) OR
        (status = 'COMPLETED' AND ended_at IS NOT NULL AND paused_at IS NULL)
    ),
    CONSTRAINT ck_focus_sessions_paused CHECK (total_paused_seconds >= 0),
    CONSTRAINT ck_focus_sessions_pause_time CHECK (paused_at IS NULL OR paused_at >= started_at),
    CONSTRAINT ck_focus_sessions_end_time CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT ck_focus_sessions_duration CHECK (
        ended_at IS NULL OR TIMESTAMPDIFF(SECOND, started_at, ended_at) >= total_paused_seconds
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

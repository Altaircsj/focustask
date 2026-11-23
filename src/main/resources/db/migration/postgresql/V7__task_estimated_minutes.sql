ALTER TABLE tasks
    ADD COLUMN estimated_minutes INTEGER,
    ADD CONSTRAINT ck_tasks_estimated_minutes CHECK (estimated_minutes IS NULL OR estimated_minutes > 0);

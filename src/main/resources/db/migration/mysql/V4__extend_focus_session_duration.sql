ALTER TABLE focus_sessions
    DROP CHECK ck_focus_sessions_plan,
    ADD CONSTRAINT ck_focus_sessions_planned_minutes CHECK (planned_minutes BETWEEN 1 AND 240);

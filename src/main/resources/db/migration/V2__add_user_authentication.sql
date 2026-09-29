-- Development transition: apply to an empty users table. No legacy credentials are generated.
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NOT NULL,
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));

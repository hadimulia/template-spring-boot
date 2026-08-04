-- Allow the same username in multiple schools (e.g. 'admin' exists in every
-- school DB). Uniqueness is now per (school_id, username) instead of global.
ALTER TABLE school_users DROP CONSTRAINT school_users_username_key;

CREATE UNIQUE INDEX uk_school_users_school_username
    ON school_users(school_id, username) WHERE deleted = false;

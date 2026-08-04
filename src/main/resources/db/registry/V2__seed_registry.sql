-- Seed the DEFAULT school and its admin login index.
-- The DEFAULT school's database is sims_default, auto-created on first login.

INSERT INTO tenants (code, name, db_name, description, status, created_by)
VALUES ('DEFAULT', 'Default', 'sims_default', 'Default tenant', 'ACTIVE', 'system');

-- The admin user row lives in sims_default.users (id 1, seeded by V3 of the
-- school migration set). school_users just indexes it globally for login.
INSERT INTO school_users (tenant_id, user_id, username, enabled, created_by)
SELECT t.id, 1, 'admin', true, 'system'
FROM tenants t
WHERE t.code = 'DEFAULT';

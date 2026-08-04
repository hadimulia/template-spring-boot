-- Database-per-tenant migration: the school database no longer needs
-- row-level tenant_id columns or the tenants table. Isolation is physical
-- (each school owns its own database). The TENANT_* permissions and the
-- /tenants menu seeded in V17 are KEPT - the school-management page now reads
-- from the registry database instead of a local tenants table.

-- 1. Drop FK constraints referencing tenants(id) before dropping the columns.
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_tenant;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS fk_user_roles_tenant;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fk_audit_logs_tenant;
ALTER TABLE approval_requests DROP CONSTRAINT IF EXISTS fk_approval_requests_tenant;
ALTER TABLE file_uploads DROP CONSTRAINT IF EXISTS fk_file_uploads_tenant;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS fk_notifications_tenant;

-- 2. Drop the tenant_id columns (drops the per-column indexes too).
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE user_roles DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE approval_requests DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE file_uploads DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE notifications DROP COLUMN IF EXISTS tenant_id;

-- 3. Drop the remaining tenant filter indexes and the tenants table.
DROP INDEX IF EXISTS idx_users_tenant;
DROP INDEX IF EXISTS idx_user_roles_tenant;
DROP INDEX IF EXISTS idx_audit_logs_tenant;
DROP INDEX IF EXISTS idx_approval_requests_tenant;
DROP INDEX IF EXISTS idx_file_uploads_tenant;
DROP INDEX IF EXISTS idx_notifications_tenant;
DROP TABLE IF EXISTS tenants;

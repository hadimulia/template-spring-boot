-- Phase 0.6: rename the tenant-management feature to school-management.
-- The registry holds the school list; each school owns its own database.

-- 1. Rename TENANT_* permissions to SCHOOL_*.
INSERT INTO permissions (code, description, created_by)
VALUES ('SCHOOL_VIEW', 'View schools', 'system'),
       ('SCHOOL_CREATE', 'Create school', 'system'),
       ('SCHOOL_EDIT', 'Edit school', 'system'),
       ('SCHOOL_DELETE', 'Delete school', 'system')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('SCHOOL_VIEW', 'SCHOOL_CREATE', 'SCHOOL_EDIT', 'SCHOOL_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 2. Point the management menu at the new /schools URL with the school i18n key.
UPDATE menus SET url = '/schools', i18n_key = 'sidebar.school' WHERE url = '/tenants' AND deleted = false;

-- 3. Old TENANT_* permissions are kept (harmless) so no existing grant breaks.

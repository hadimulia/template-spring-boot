-- Multi-tenancy: shared database, shared schema, tenant_id column on per-tenant business tables.
-- Tenant is resolved from the authenticated user (users.tenant_id).
-- Roles, permissions, and menus are GLOBAL shared reference data (no tenant_id).

-- 1. Create tenants table
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

-- 2. Add tenant_id to per-tenant business tables
ALTER TABLE users ADD COLUMN tenant_id BIGINT;
ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT;
ALTER TABLE audit_logs ADD COLUMN tenant_id BIGINT;
ALTER TABLE approval_requests ADD COLUMN tenant_id BIGINT;
ALTER TABLE file_uploads ADD COLUMN tenant_id BIGINT;
ALTER TABLE notifications ADD COLUMN tenant_id BIGINT;

-- 3. Seed default tenant
INSERT INTO tenants (code, name, description, status, created_by)
VALUES ('DEFAULT', 'Default', 'Default tenant', 'ACTIVE', 'system');

-- 4. Backfill existing rows to the default tenant
UPDATE users SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');
UPDATE user_roles SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');
UPDATE audit_logs SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');
UPDATE approval_requests SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');
UPDATE file_uploads SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');
UPDATE notifications SET tenant_id = (SELECT id FROM tenants WHERE code = 'DEFAULT');

-- 5. Add NOT NULL constraints and foreign keys
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE user_roles ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE audit_logs ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE approval_requests ADD CONSTRAINT fk_approval_requests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE approval_requests ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE file_uploads ADD CONSTRAINT fk_file_uploads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE file_uploads ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE notifications ADD CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE notifications ALTER COLUMN tenant_id SET NOT NULL;

-- 6. Indexes for tenant filtering
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_approval_requests_tenant ON approval_requests(tenant_id);
CREATE INDEX idx_file_uploads_tenant ON file_uploads(tenant_id);
CREATE INDEX idx_notifications_tenant ON notifications(tenant_id);

-- 7. Tenant management permissions
INSERT INTO permissions (code, description, created_by)
VALUES ('TENANT_VIEW', 'View tenants', 'system'),
       ('TENANT_CREATE', 'Create tenant', 'system'),
       ('TENANT_EDIT', 'Edit tenant', 'system'),
       ('TENANT_DELETE', 'Delete tenant', 'system')
ON CONFLICT (code) DO NOTHING;

-- 8. Tenant management menu under Master (parent_id = 2, sort_order = 10)
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
VALUES (2, 'Tenant', '/tenants', 'bi-building', 10, true, 'system')
ON CONFLICT DO NOTHING;

-- 8b. Set i18n key for the tenant menu
UPDATE menus SET i18n_key = 'sidebar.tenant' WHERE url = '/tenants' AND deleted = false;

-- 9. Assign tenant permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('TENANT_VIEW', 'TENANT_CREATE', 'TENANT_EDIT', 'TENANT_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 10. Assign tenant menu to ADMIN role
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/tenants'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(20) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    description VARCHAR(500),
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

CREATE INDEX idx_audit_logs_performed_at ON audit_logs(performed_at DESC);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs(performed_by);

-- Add audit trail permissions
INSERT INTO permissions (code, description, created_by)
VALUES ('AUDIT_VIEW', 'View audit trail', 'system')
ON CONFLICT (code) DO NOTHING;

-- Add audit trail to sidebar (parent_id = 2 = Master, sort_order = 6)
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
VALUES (2, 'Audit Trail', '/audit-logs', 'bi-journal-text', 6, true, 'system')
ON CONFLICT DO NOTHING;

-- Assign to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('AUDIT_VIEW')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/audit-logs'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

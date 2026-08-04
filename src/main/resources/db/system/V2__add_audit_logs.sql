-- The system realm reuses the school management pages, whose @Auditable
-- aspects write to audit_logs. Without this table, system-admin actions
-- (create/update/delete school) throw "relation audit_logs does not exist".
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

-- Grant AUDIT_VIEW to the SYSTEM role so it can view the audit trail.
INSERT INTO permissions (code, description, created_by)
VALUES ('AUDIT_VIEW', 'View audit trail', 'system')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'SYSTEM'
  AND p.code IN ('AUDIT_VIEW')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

CREATE TABLE approval_requests (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    request_data TEXT,
    submitted_by VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(500),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP
);

CREATE INDEX idx_approval_status ON approval_requests(status);
CREATE INDEX idx_approval_submitted_by ON approval_requests(submitted_by);

INSERT INTO permissions (code, description, created_by)
VALUES ('APPROVAL_VIEW', 'View approval requests', 'system'),
       ('APPROVAL_REVIEW', 'Review/approve requests', 'system')
ON CONFLICT (code) DO NOTHING;

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT id, 'Approvals', '/approvals', 'bi-check2-circle', 7, true, 'system'
FROM menus WHERE url = '/audit-logs' AND deleted = false
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('APPROVAL_VIEW', 'APPROVAL_REVIEW')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN' AND m.url = '/approvals'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

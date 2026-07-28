-- Add permission management permissions
INSERT INTO permissions (code, description, created_by)
VALUES ('PERMISSION_VIEW', 'View permissions', 'system'),
       ('PERMISSION_CREATE', 'Create permission', 'system'),
       ('PERMISSION_EDIT', 'Edit permission', 'system'),
       ('PERMISSION_DELETE', 'Delete permission', 'system')
ON CONFLICT (code) DO NOTHING;

-- Add permission management menu under Master (parent_id = 2)
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
VALUES (2, 'Permission', '/permissions', 'bi-shield-lock', 5, true, 'system')
ON CONFLICT DO NOTHING;

-- Assign new permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('PERMISSION_VIEW', 'PERMISSION_CREATE', 'PERMISSION_EDIT', 'PERMISSION_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign new menu to ADMIN role
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/permissions'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

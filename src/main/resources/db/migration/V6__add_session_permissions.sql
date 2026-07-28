-- Add session management permissions
INSERT INTO permissions (code, description, created_by)
VALUES ('SESSION_VIEW', 'View active sessions', 'system'),
       ('SESSION_KICK', 'Force logout user session', 'system')
ON CONFLICT (code) DO NOTHING;

-- Add session management menu under Master (parent_id = 2)
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
VALUES (2, 'Session', '/sessions', 'bi-people', 4, true, 'system')
ON CONFLICT DO NOTHING;

-- Assign new permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('SESSION_VIEW', 'SESSION_KICK')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Assign new menu to ADMIN role
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/sessions'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

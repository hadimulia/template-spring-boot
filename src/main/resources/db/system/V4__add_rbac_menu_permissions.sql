-- System realm RBAC: seed authorities and menus for role/permission/menu/
-- system-user management, and rename the school-user menu.

-- 1. Permissions
INSERT INTO permissions (code, description, created_by) VALUES
    ('USER_VIEW', 'View system users', 'system'),
    ('USER_CREATE', 'Create system user', 'system'),
    ('USER_EDIT', 'Edit system user', 'system'),
    ('USER_DELETE', 'Delete system user', 'system'),
    ('ROLE_VIEW', 'View roles', 'system'),
    ('ROLE_CREATE', 'Create role', 'system'),
    ('ROLE_EDIT', 'Edit role', 'system'),
    ('ROLE_DELETE', 'Delete role', 'system'),
    ('PERMISSION_VIEW', 'View permissions', 'system'),
    ('PERMISSION_CREATE', 'Create permission', 'system'),
    ('PERMISSION_EDIT', 'Edit permission', 'system'),
    ('PERMISSION_DELETE', 'Delete permission', 'system'),
    ('MENU_VIEW', 'View menus', 'system'),
    ('MENU_CREATE', 'Create menu', 'system'),
    ('MENU_EDIT', 'Edit menu', 'system'),
    ('MENU_DELETE', 'Delete menu', 'system')
ON CONFLICT (code) DO NOTHING;

-- 2. Menus
UPDATE menus SET name = 'User Sekolah' WHERE url = '/system/users';

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by) VALUES
    (NULL, 'User System', '/system/users/system', 'bi-person-gear', 3, true, 'system'),
    (NULL, 'Role', '/system/roles', 'bi-shield', 4, true, 'system'),
    (NULL, 'Permission', '/system/permissions', 'bi-key', 5, true, 'system'),
    (NULL, 'Menu Management', '/system/menus', 'bi-list', 6, true, 'system')
ON CONFLICT DO NOTHING;

-- 3. Grant all new permissions to SYSTEM
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'SYSTEM'
  AND p.code IN ('USER_VIEW','USER_CREATE','USER_EDIT','USER_DELETE',
                 'ROLE_VIEW','ROLE_CREATE','ROLE_EDIT','ROLE_DELETE',
                 'PERMISSION_VIEW','PERMISSION_CREATE','PERMISSION_EDIT','PERMISSION_DELETE',
                 'MENU_VIEW','MENU_CREATE','MENU_EDIT','MENU_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 4. Grant new menus to SYSTEM
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'SYSTEM'
  AND m.url IN ('/system/users/system','/system/roles','/system/permissions','/system/menus')
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

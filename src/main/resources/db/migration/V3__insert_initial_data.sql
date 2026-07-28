-- Default admin user (password: admin123)
INSERT INTO users (username, password, fullname, email, enabled, created_by)
VALUES ('admin', '$2a$10$i8.1.j4j.iZp2GkL7c5y8uJndnZJ0Z0mF/b.yH/y.xY1c2W5N6OqG', 'System Administrator', 'admin@example.com', true, 'system');

-- Default roles
INSERT INTO roles (name, description, created_by)
VALUES
    ('ADMIN', 'System Administrator', 'system'),
    ('USER', 'Regular User', 'system');

-- Default permissions
INSERT INTO permissions (code, description, created_by)
VALUES
    ('USER_VIEW', 'View users', 'system'),
    ('USER_CREATE', 'Create user', 'system'),
    ('USER_EDIT', 'Edit user', 'system'),
    ('USER_DELETE', 'Delete user', 'system'),
    ('ROLE_VIEW', 'View roles', 'system'),
    ('ROLE_CREATE', 'Create role', 'system'),
    ('ROLE_EDIT', 'Edit role', 'system'),
    ('ROLE_DELETE', 'Delete role', 'system'),
    ('MENU_VIEW', 'View menus', 'system'),
    ('MENU_CREATE', 'Create menu', 'system'),
    ('MENU_EDIT', 'Edit menu', 'system'),
    ('MENU_DELETE', 'Delete menu', 'system');

-- Default menus
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
VALUES
    (NULL, 'Dashboard', '/dashboard', 'bi-speedometer2', 1, true, 'system'),
    (NULL, 'Master', NULL, 'bi-folder', 2, true, 'system'),
    (2, 'User', '/users', 'bi-person', 1, true, 'system'),
    (2, 'Role', '/roles', 'bi-shield', 2, true, 'system'),
    (2, 'Menu', '/menus', 'bi-list', 3, true, 'system');

-- Assign admin role all permissions
INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- Assign admin role all menus
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN';

-- Assign admin user to admin role
INSERT INTO user_roles (user_id, role_id, created_by)
SELECT u.id, r.id, 'system'
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';

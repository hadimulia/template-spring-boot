-- Add a "Users" menu to the system realm pointing at the cross-school user
-- management page. The SYSTEM role gets the menu grant so system admins can
-- reach /system/users.
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT NULL, 'Users', '/system/users', 'bi-people', 2, true, 'system'
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE url = '/system/users');

INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'SYSTEM'
  AND m.url = '/system/users'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

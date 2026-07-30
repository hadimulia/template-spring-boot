-- Fix sort orders and add missing feature menus under Master

-- Ensure Permission has correct sort_order
UPDATE menus SET sort_order = 4 WHERE url = '/permissions' AND deleted = false AND sort_order != 4;

-- Fix duplicate Permission: soft-deleted id=8 exists alongside active id=9
-- Ensure the active one has correct parent and sort
UPDATE menus SET parent_id = (SELECT id FROM menus WHERE name = 'Master' AND parent_id IS NULL AND deleted = false)
WHERE url = '/permissions' AND deleted = false AND parent_id IS NULL;

-- Ensure Session has correct sort_order=5
UPDATE menus SET sort_order = 5 WHERE url = '/sessions' AND deleted = false AND sort_order != 5;

-- Ensure Approvals is under Master (fix V10 bug that set parent to audit-log id)
UPDATE menus SET parent_id = (SELECT id FROM menus WHERE name = 'Master' AND parent_id IS NULL AND deleted = false)
WHERE url = '/approvals' AND deleted = false
  AND parent_id != (SELECT id FROM menus WHERE name = 'Master' AND parent_id IS NULL AND deleted = false);

-- Add File Manager menu under Master if missing
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT id, 'File Manager', '/files', 'bi-folder2', 8, true, 'system'
FROM menus WHERE name = 'Master' AND parent_id IS NULL AND deleted = false
AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/files' AND deleted = false);

-- Add Notifications menu under Master if missing
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT id, 'Notifications', '/notifications', 'bi-bell', 9, true, 'system'
FROM menus WHERE name = 'Master' AND parent_id IS NULL AND deleted = false
AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/notifications' AND deleted = false);

-- Assign File Manager role_menu for ADMIN if missing
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN' AND m.url = '/files' AND m.deleted = false
AND NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- Assign Notifications role_menu for ADMIN if missing
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN' AND m.url = '/notifications' AND m.deleted = false
AND NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

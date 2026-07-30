-- Fix menu tree: ensure all feature menus are under Master (parent_id=2)
-- V10 incorrectly set Approvals parent_id to audit-log's id instead of Master's id

-- Fix Approvals menu: move under Master (parent_id=2) if it has wrong parent
UPDATE menus
SET parent_id = (SELECT id FROM menus WHERE url IS NULL AND name = 'Master' AND deleted = false)
WHERE url = '/approvals'
  AND parent_id != (SELECT id FROM menus WHERE url IS NULL AND name = 'Master' AND deleted = false)
  AND deleted = false;

-- Add File Manager menu under Master
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT id, 'File Manager', '/files', 'bi-folder2', 8, true, 'system'
FROM menus WHERE url IS NULL AND name = 'Master' AND deleted = false
AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/files' AND deleted = false);

-- Add Notifications menu under Master
INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT id, 'Notifications', '/notifications', 'bi-bell', 9, true, 'system'
FROM menus WHERE url IS NULL AND name = 'Master' AND deleted = false
AND NOT EXISTS (SELECT 1 FROM menus WHERE url = '/notifications' AND deleted = false);

-- Assign File Manager menu to ADMIN role
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/files'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

-- Assign Notifications menu to ADMIN role
INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system'
FROM roles r, menus m
WHERE r.name = 'ADMIN'
  AND m.url = '/notifications'
  AND NOT EXISTS (
    SELECT 1 FROM role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
);

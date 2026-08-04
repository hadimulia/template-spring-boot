-- School databases must NOT expose school-management to their own admins.
-- The /schools menu and SCHOOL_* permissions belong only to the system realm
-- (sims_system). Remove them from every school database.

-- 1. Drop grants first (junction tables reference the rows being deleted).
DELETE FROM role_menus
WHERE menu_id IN (SELECT id FROM menus WHERE url = '/schools');

DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code LIKE 'SCHOOL\_%');

-- 2. Drop the menu row(s).
DELETE FROM menus WHERE url = '/schools';

-- 3. Drop the SCHOOL_* permissions.
DELETE FROM permissions WHERE code LIKE 'SCHOOL\_%';

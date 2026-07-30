-- Add i18n_key column to menus for message properties lookup
ALTER TABLE menus ADD COLUMN IF NOT EXISTS i18n_key VARCHAR(100) DEFAULT '';

-- Seed i18n keys for existing menus
UPDATE menus SET i18n_key = 'sidebar.dashboard' WHERE url = '/' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.dashboard' WHERE url = '/dashboard' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.user' WHERE url = '/users' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.role' WHERE url = '/roles' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.menu' WHERE url = '/menus' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.permission' WHERE url = '/permissions' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.session' WHERE url = '/sessions' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.audit' WHERE url = '/audit-logs' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.approval' WHERE url = '/approvals' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.file' WHERE url = '/files' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.notification' WHERE url = '/notifications' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.master' WHERE url = '' AND name = 'Master' AND deleted = false AND (i18n_key IS NULL OR i18n_key = '');

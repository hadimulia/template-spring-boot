-- The school MenuMapper queries reference menus.i18n_key (and parentI18nKey);
-- the system realm's menus table (created by V1) lacks it. Add the column and
-- seed keys for the system menus so the shared mappers work against sims_system.
ALTER TABLE menus ADD COLUMN IF NOT EXISTS i18n_key VARCHAR(100) DEFAULT '';

UPDATE menus SET i18n_key = 'sidebar.school' WHERE url = '/schools' AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.schoolUser' WHERE url = '/system/users' AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.systemUser' WHERE url = '/system/users/system' AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.role' WHERE url = '/system/roles' AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.permission' WHERE url = '/system/permissions' AND (i18n_key IS NULL OR i18n_key = '');
UPDATE menus SET i18n_key = 'sidebar.menu' WHERE url = '/system/menus' AND (i18n_key IS NULL OR i18n_key = '');

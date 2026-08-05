-- Shared mappers (UserService, RoleService, PermissionService, MenuService)
-- expect audit/soft-delete columns on the RBAC tables. The system realm's V1
-- created the minimal shapes without them; add them to match the school set.

ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

ALTER TABLE role_permissions
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

ALTER TABLE role_menus
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

DO $$ BEGIN
    ALTER TABLE role_permissions ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;
DO $$ BEGIN
    ALTER TABLE role_menus ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;

-- Main tables may lack the audit columns created by school migrations.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;

ALTER TABLE menus
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50),
    ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP;
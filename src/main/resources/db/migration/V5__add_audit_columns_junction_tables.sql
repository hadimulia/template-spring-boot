-- Add soft-delete and version columns to junction tables
-- role_permissions already has created_date from V2

ALTER TABLE user_roles
    ADD COLUMN deleted BOOLEAN DEFAULT false,
    ADD COLUMN version INT DEFAULT 0,
    ADD COLUMN updated_by VARCHAR(50),
    ADD COLUMN updated_date TIMESTAMP;

ALTER TABLE role_permissions
    ADD COLUMN deleted BOOLEAN DEFAULT false,
    ADD COLUMN version INT DEFAULT 0,
    ADD COLUMN updated_by VARCHAR(50),
    ADD COLUMN updated_date TIMESTAMP;

ALTER TABLE role_menus
    ADD COLUMN deleted BOOLEAN DEFAULT false,
    ADD COLUMN version INT DEFAULT 0,
    ADD COLUMN updated_by VARCHAR(50),
    ADD COLUMN updated_date TIMESTAMP;

-- role_permissions and role_menus may already have created_date, add if missing
DO $$ BEGIN
    ALTER TABLE role_permissions ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE role_menus ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
EXCEPTION WHEN duplicate_column THEN NULL;
END $$;

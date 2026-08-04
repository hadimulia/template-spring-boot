-- Dedicated system realm for master administrators who control all schools.
-- Mirrors the school realm's user/RBAC shape so existing mappers work unchanged.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    enabled BOOLEAN DEFAULT true,
    account_locked BOOLEAN DEFAULT false,
    login_attempts INT DEFAULT 0,
    last_login TIMESTAMP,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE menus (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES menus(id),
    name VARCHAR(50) NOT NULL,
    url VARCHAR(255),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    visible BOOLEAN DEFAULT true,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    permission_id BIGINT NOT NULL REFERENCES permissions(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

CREATE TABLE role_menus (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    menu_id BIGINT NOT NULL REFERENCES menus(id),
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);

-- Seed: system admin (password: admin123, same corrected hash as school V4)
INSERT INTO users (username, password, fullname, email, enabled, created_by)
VALUES ('admin', '$2a$10$mL1Onwq9YlVNKfLngsghbujC4ueZSSaT8kJ/Urz/Z6O.5W1e1C4Kq',
        'System Administrator', 'admin@system', true, 'system');

INSERT INTO roles (name, description, created_by) VALUES ('SYSTEM', 'System Administrator', 'system');

INSERT INTO permissions (code, description, created_by) VALUES
    ('SCHOOL_VIEW', 'View schools', 'system'),
    ('SCHOOL_CREATE', 'Create school', 'system'),
    ('SCHOOL_EDIT', 'Edit school', 'system'),
    ('SCHOOL_DELETE', 'Delete school', 'system');

INSERT INTO menus (parent_id, name, url, icon, sort_order, visible, created_by)
SELECT NULL, 'Schools', '/schools', 'bi-buildings', 1, true, 'system';

INSERT INTO role_permissions (role_id, permission_id, created_by)
SELECT r.id, p.id, 'system' FROM roles r, permissions p
WHERE r.name = 'SYSTEM' AND p.code IN ('SCHOOL_VIEW','SCHOOL_CREATE','SCHOOL_EDIT','SCHOOL_DELETE');

INSERT INTO role_menus (role_id, menu_id, created_by)
SELECT r.id, m.id, 'system' FROM roles r, menus m
WHERE r.name = 'SYSTEM' AND m.url = '/schools';

INSERT INTO user_roles (user_id, role_id, created_by)
SELECT u.id, r.id, 'system' FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'SYSTEM';

-- Database-per-tenant registry.
-- Holds the school list (tenants) and the global login index that maps a
-- username to a school. Each school owns its own database (sims_<code>).

-- 1. Schools registry (kept as "tenants" to match the existing entity/mapper).
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    db_name VARCHAR(63) UNIQUE NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false,
    version INT DEFAULT 0
);

-- 2. Global login index: username -> school + user row inside that school DB.
-- The actual credentials/profile/RBAC stay in the school database (users table).
CREATE TABLE school_users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    user_id BIGINT NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    updated_date TIMESTAMP,
    deleted BOOLEAN DEFAULT false
);

CREATE INDEX idx_school_users_tenant ON school_users(tenant_id);
CREATE INDEX idx_school_users_username ON school_users(username);

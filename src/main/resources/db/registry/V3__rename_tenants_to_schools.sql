-- Rename the registry "tenants" concept to "schools" for clarity.
-- The registry holds the school list; each school owns its own database.

ALTER TABLE tenants RENAME TO schools;
ALTER TABLE schools RENAME CONSTRAINT tenants_pkey TO schools_pkey;

ALTER TABLE school_users DROP CONSTRAINT IF EXISTS school_users_tenant_id_fkey;
ALTER TABLE school_users RENAME COLUMN tenant_id TO school_id;
ALTER TABLE school_users
    ADD CONSTRAINT fk_school_users_school FOREIGN KEY (school_id) REFERENCES schools(id);

ALTER INDEX idx_school_users_tenant RENAME TO idx_school_users_school;

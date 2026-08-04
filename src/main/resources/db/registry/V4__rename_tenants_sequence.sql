-- Finish the tenants->schools rename: the id sequence and the unique
-- constraints still carry the old "tenants" names. The registry GenId
-- (RegistrySequenceGenId) reads nextval('<table>_id_seq'), so the sequence
-- must match the table name for school inserts to generate ids.

ALTER SEQUENCE tenants_id_seq RENAME TO schools_id_seq;

ALTER TABLE schools RENAME CONSTRAINT tenants_code_key TO schools_code_key;
ALTER TABLE schools RENAME CONSTRAINT tenants_db_name_key TO schools_db_name_key;

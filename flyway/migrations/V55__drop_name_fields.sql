-- V55__drop_name_fields.sql

-- Version Control
-- V1.0  	Matthew Harman  02/06/2026	Initial Version
--

-- Modify the existing tables
-- Drop existing NAME_ADDRESS_NAME_OR_PERSON_CHK constraint on the name_address table
ALTER TABLE name_address
	DROP CONSTRAINT IF EXISTS name_address_name_or_person_chk;

-- Recreate it with the new fields
ALTER TABLE IF EXISTS name_address
    ADD CONSTRAINT name_address_name_or_person_chk CHECK 
		(NULLIF(btrim(name::text), ''::text) IS NOT NULL AND NULLIF(btrim(title::text), ''::text) IS NULL AND NULLIF(btrim(first_name::text), ''::text) IS NULL AND NULLIF(btrim(middle_name::text), ''::text) IS NULL AND NULLIF(btrim(last_name::text), ''::text) IS NULL 
		OR NULLIF(btrim(name::text), ''::text) IS NULL AND NULLIF(btrim(first_name::text), ''::text) IS NOT NULL AND NULLIF(btrim(last_name::text), ''::text) IS NOT NULL AND (NULLIF(btrim(middle_name::text), ''::text) IS NULL 
		OR NULLIF(btrim(first_name::text), ''::text) IS NOT NULL))
    NOT VALID;

-- Drop the old fields from NAME_ADDRESS table
-- forename_1
-- forename_2
-- forename_3
-- surname
ALTER TABLE name_address
	DROP COLUMN IF EXISTS forename_1,
	DROP COLUMN IF EXISTS forename_2,
	DROP COLUMN IF EXISTS forename_3,
	DROP COLUMN IF EXISTS surname;

-- Create a unique index on the retention_policy table for the combination
-- of dj_dj_id and config_key
CREATE UNIQUE INDEX IF NOT EXISTS retention_policy_dj_config_key_uix 
ON retention_policy (dj_dj_id, config_key);

-- Recreate the dropped index
CREATE INDEX na_upp_s_idx ON name_address (upper(last_name));

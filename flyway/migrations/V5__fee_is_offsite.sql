-- Add is_offsite field to fee table

-- Version Control
-- V1.0  	Matthew Harman  12/08/2025	Initial Version
-- V2.0  	Matthew Harman  16/09/2025	Added code for testing script applied
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

ALTER TABLE fee ADD COLUMN is_offsite boolean DEFAULT false;

-- Insert our test data for V5
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('5', 'test_support', 'check_schema_objects_v5_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v5_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check for existence of fee.is_offsite column
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fee' and column_name = 'is_offsite') THEN
		RAISE EXCEPTION 'Table: fee  Column: is_offsite is missing';
	END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
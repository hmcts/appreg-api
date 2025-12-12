-- Add ID field of UUID to APP_LIST_ENTRY_RESOLUTIONS table as per ARCPOC-811:
--
--  application_list_entries

-- Version Control
-- V1.0  	Matthew Harman      11/12/2025	Initial version
--

-- Add uuid fields to APP_LIST_ENTRY_RESOLUTIONS table
ALTER TABLE app_list_entry_resolutions ADD COLUMN id UUID DEFAULT gen_random_uuid() UNIQUE;


-- Insert our test data for V19
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('19', 'test_support', 'check_schema_objects_v19_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v19_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check app_list_entry_resolutions.id is uuid
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_list_entry_resolutions' and column_name = 'id' and data_type = 'uuid') THEN
		RAISE EXCEPTION 'Table: app_list_entry_resolutions  Column: id is not a uuid';
	END IF;


	-- If all checks pass, do nothing (test passes)
END $$;
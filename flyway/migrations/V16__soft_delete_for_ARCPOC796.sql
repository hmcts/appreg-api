-- Add delete_by, delete_date and is_deleted to tables as per ARCPOC-796:
--
--  application_list_entries

-- Version Control
-- V1.0  	Matthew Harman      04/12/2025	Initial version
--

-- Application_List_Entries Table
ALTER TABLE application_list_entries
ADD COLUMN delete_by VARCHAR(73),
ADD COLUMN delete_date TIMESTAMP,
ADD COLUMN is_deleted CHAR(1) DEFAULT '0' CHECK (is_deleted IN ('1', '0'));


-- Insert our test data for V16
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('16', 'test_support', 'check_schema_objects_v16_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v16_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check application_list_entries.delete_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_list_entries' and column_name = 'delete_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: application_list_entries  Column: delete_by is not a varchar(73)';
	END IF;

	-- Check application_list_entries.delete_date is timestamp
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_list_entries' and column_name = 'delete_date' and data_type = 'timestamp without time zone') THEN
		RAISE EXCEPTION 'Table: application_list_entries  Column: delete_date is not a timestamp';
	END IF;

	-- Check application_list_entries.is_deleted is char(1)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_list_entries' and column_name = 'is_deleted' and data_type = 'character' and character_maximum_length = 1) THEN
		RAISE EXCEPTION 'Table: application_list_entries  Column: is_deleted is not a char(1)';
	END IF;


	-- If all checks pass, do nothing (test passes)
END $$;
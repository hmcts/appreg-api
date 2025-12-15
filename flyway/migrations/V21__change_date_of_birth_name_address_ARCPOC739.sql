-- Change date_of_birth in NAME_ADDRESS from TIMESTAMP to DATE as per ARCPOC-739:
--
-- Version Control
-- V1.0  	Matthew Harman      12/12/2025	Initial version
--

-- Alter date_of_birth field in NAME_ADDRESS from TIMESTAMP to DATE
ALTER TABLE name_address ALTER COLUMN date_of_birth TYPE DATE;

-- Insert our test data for V21
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('21', 'test_support', 'check_schema_objects_v21_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v21_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check name_address.date_of_birth is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'name_address' and column_name = 'date_of_birth' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: name_address  Column: date_of_birth is not a date field';
	END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
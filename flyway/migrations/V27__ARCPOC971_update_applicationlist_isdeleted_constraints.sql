-- Modify application_list_entries and application_lists tables to use 'Y'/'N' instead of '1'/'0' for is_deleted

-- Version Control
-- V1.0  	 Phil Head      21/01/2026	Initial version
-- V2.0      Matthew Harman 22/01/2026	Added test procedures around the script

--



ALTER TABLE application_list_entries DROP CONSTRAINT application_list_entries_is_deleted_check;

update application_list_entries set is_deleted = 'Y' where is_deleted = '1';
update application_list_entries set is_deleted = 'N' where is_deleted = '0';

ALTER TABLE application_list_entries ALTER COLUMN is_deleted SET DEFAULT 'N';
ALTER TABLE application_list_entries ADD CONSTRAINT application_list_entries_is_deleted_check CHECK (is_deleted = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]));

ALTER TABLE application_lists DROP CONSTRAINT application_lists_is_deleted_check;

update application_lists set is_deleted = 'Y' where is_deleted = '1';
update application_lists set is_deleted = 'N' where is_deleted = '0';

ALTER TABLE application_lists ALTER COLUMN is_deleted SET DEFAULT 'N';
ALTER TABLE application_lists ADD CONSTRAINT application_lists_is_deleted_check CHECK (is_deleted = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]));

-- Insert our test data for V26
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('27', 'test_support', 'check_schema_objects_v27_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v27_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check application_lists table has no records with is_deleted = '0' or '1'
    IF EXISTS (SELECT 1 FROM application_lists WHERE is_deleted IN ('0', '1')) THEN
        RAISE EXCEPTION 'Table application_lists contains invalid is_deleted values (0 or 1)';
    END IF;

    -- Check application_list_entries table has no records with is_deleted = '0' or '1'
    IF EXISTS (SELECT 1 FROM application_list_entries WHERE is_deleted IN ('0', '1')) THEN
        RAISE EXCEPTION 'Table application_list_entries contains invalid is_deleted values (0 or 1)';
    END IF;
END $$;

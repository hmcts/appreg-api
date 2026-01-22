-- History
-- Matthew Harman       V1.0    22/01/2026      Ensure CODE is only RE and NA in
--                                              name_address table
--
UPDATE name_address SET code = 'NA' WHERE code = 'AP';

-- Insert our test data for V29
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('29', 'test_support', 'check_data_expected_v29_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v29_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check that code values in name_address are only 'RE' or 'NA'
    IF EXISTS (SELECT 1 FROM name_address WHERE code NOT IN ('RE', 'NA')) THEN
        RAISE EXCEPTION 'Name address table contains code values other than RE or NA';
    END IF;

    -- If all checks pass, do nothing (test passes)
END $$;

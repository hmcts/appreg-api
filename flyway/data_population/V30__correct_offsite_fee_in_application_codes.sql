-- History
-- Matthew Harman       V1.0    27/01/2026      Ensure ac_fee_reference not populated for
--                                              offsite fees in application_codes table
--
UPDATE application_codes SET ac_fee_reference = NULL 
WHERE ac_fee_reference in (select fee_reference from appreg.fee where is_offsite = true);

-- Insert our test data for V30
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('30', 'test_support', 'check_data_expected_v30_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v30_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check that we have no NULLs for offsite fee records
    IF EXISTS (SELECT 1 FROM application_codes ac
               WHERE ac.ac_fee_reference IS NOT NULL
               AND ac.ac_fee_reference IN (SELECT fee_reference FROM appreg.fee WHERE is_offsite = true)) THEN
        RAISE EXCEPTION 'application_codes has unexpected references to offsite fees';
    END IF;

    -- If all checks pass, do nothing (test passes)
END $$;

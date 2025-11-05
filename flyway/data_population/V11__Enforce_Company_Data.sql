-- History
-- Matthew Harman       V1.0    04/11/2025      Initial Version
--
-- This script enforces the company data in the
-- Name_Address and Standard_Applicants tables
--
-- Name_Address:
-- Company:  
--      Name:   populated
--      Title:  NULL
--      Forename_1: NULL    
--      Forename_2: NULL
--      Forename_3: NULL
--      Surname:    NULL
--
-- Person:
--      Name:   NULL
--      Title:  populated   
--      Forename_1: populated
--      Forename_2: populated
--      Forename_3: populated   
--      Surname:    populated
--
-- Standard_Applicants:
-- Company:
--      Name:   populated
--      Title:  NULL    
--      Forename_1: NULL
--      Forename_2: NULL
--      Forename_3: NULL
--      Surname:    NULL
--
-- Person:
--      Name:   NULL
--      Title:  populated   
--      Forename_1: populated       
--      Forename_2: populated
--      Forename_3: populated
--      Surname:    populated
--

-- Correct Name_Address entries for the Companies
UPDATE name_address
SET
    title = NULL,
    forename_1 = NULL,
    forename_2 = NULL,
    forename_3 = NULL,
    surname = NULL
WHERE
    forename_1 IS NULL;

-- Correct Name_Address entries for the Persons
UPDATE name_address
SET
    name = NULL
WHERE
    forename_1 IS NOT NULL;

-- Correct Name_Address entry 3 as it has no title
UPDATE name_address
SET
    title = 'Mr'
WHERE   
    na_id = 3;

-- Correct Standard_Applicants entries for the Companies
UPDATE standard_applicants
SET
    title = NULL,
    forename_1 = NULL,
    forename_2 = NULL,  
    forename_3 = NULL,
    surname = NULL
WHERE   
    sa_id >= 31;

-- Correct Standard_Applicants entries for the Persons
UPDATE standard_applicants
SET
    name = NULL
WHERE   
    sa_id < 31;

-- Insert our test data for V11
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('11', 'test_support', 'check_schema_objects_v11_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v11_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check records conform to Name_address as above
    IF EXISTS (SELECT 1 FROM name_address WHERE name IS NOT NULL AND forename_1 IS NOT NULL) THEN
        RAISE EXCEPTION 'Name_Address has records with both name and forename_1 populated';
    END IF;
    -- Check records conform to Standard_Applicants as above
    IF EXISTS (SELECT 1 FROM standard_applicants WHERE name IS NOT NULL AND forename_1 IS NOT NULL) THEN
        RAISE EXCEPTION 'Standard_Applicants has records with both name and forename_1 populated';
    END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
-- History
-- Matthew Harman       V1.0    18/11/2025      Move 2 records of COURT_TYPE=MAG to COURT_TYPE=CHOA
--                                              in National_Court_Houses
--                                              Create additional 20 records in Criminal_Justice_Area
--
-- Update existing records in National_Court_Houses
-- to change COURT_TYPE from MAG to CHOA
UPDATE national_court_houses
SET court_type = 'CHOA'
WHERE court_type = 'MAG';

INSERT INTO criminal_justice_area (cja_id, cja_code, cja_description) VALUES
    (300, 'A0', 'CJA Number 300'),
    (301, 'A1', 'CJA Number 301'),
    (302, 'A2', 'CJA Number 302'),
    (303, 'A3', 'CJA Number 303'),
    (304, 'A4', 'CJA Number 304'),
    (305, 'A5', 'CJA Number 305'),
    (306, 'A6', 'CJA Number 306'),
    (307, 'A7', 'CJA Number 307'),
    (308, 'A8', 'CJA Number 308'),
    (309, 'A9', 'CJA Number 309'),
    (310, 'B0', 'CJA Number 310'),
    (311, 'B1', 'CJA Number 311'),
    (312, 'B2', 'CJA Number 312'),
    (313, 'B3', 'CJA Number 313'),
    (314, 'B4', 'CJA Number 314'),
    (315, 'B5', 'CJA Number 315'),
    (316, 'B6', 'CJA Number 316'),
    (317, 'B7', 'CJA Number 317'),
    (318, 'B8', 'CJA Number 318'),
    (319, 'B9', 'CJA Number 319');
    
-- Reset the sequence for criminal_justice_area.cja_id
SELECT setval('cja_seq'::regclass, (SELECT MAX(cja_id)::bigint FROM criminal_justice_area));

-- Insert our test data for V1
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('15', 'test_support', 'check_data_expected_v15_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v15_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check zero records exist in National_Court_Houses with COURT_TYPE = 'MAG'
    IF EXISTS (SELECT 1 FROM national_court_houses WHERE court_type = 'MAG') THEN
        RAISE EXCEPTION 'Expected 0 national_court_houses with COURT_TYPE MAG, got %',
            (SELECT COUNT(*) FROM national_court_houses WHERE court_type = 'MAG');
    END IF;

    -- Check exactly 20 new records exist in Criminal_Justice_Area with cja_id 300..319
    DECLARE
        v_count int;
    BEGIN
        SELECT COUNT(*) INTO v_count
        FROM criminal_justice_area
        WHERE cja_id BETWEEN 300 AND 319;

        IF v_count <> 20 THEN
            RAISE EXCEPTION 'Expected 20 criminal_justice_area records with cja_id 300..319, got %', v_count;
        END IF;

        -- Verify each individual cja_id 300..319 exists
        FOR i IN 300..319 LOOP
            IF NOT EXISTS (SELECT 1 FROM criminal_justice_area WHERE cja_id = i) THEN
                RAISE EXCEPTION 'Missing criminal_justice_area record with cja_id %', i;
            END IF;
        END LOOP;
    END;
END $$;


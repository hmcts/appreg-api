-- History
-- Matthew Harman       V1.0    10/12/2025      Create duplicate data as defined in ticket
--                                              ARCPOC-812, to allow testing of ticket 409
--
--
-- Create duplicate Criminal Justice Areas
-- CRIMINAL_JUSTICE_AREA
-- Create 2 CJAs that are duplicates (CJA_CODE)
INSERT INTO criminal_justice_area (cja_id, cja_code, cja_description) VALUES
        (501, 'ZZ', 'CJA Number 1'),
        (502, 'ZZ', 'CJA Number 1 Duplicate')
;

-- Reset the sequence for criminal_justice_area.cja_id;
SELECT setval('cja_seq'::regclass, (SELECT MAX(cja_id)::bigint FROM criminal_justice_area));

-- Create duplicate National Court Houses
-- NATIONAL_COURT_HOUSES
-- Create 2 National Court Houses that are duplicates (COURT_LOCATION_CODE), with
-- START_DATE before TODAY and END_DATE of NULL
INSERT INTO national_court_houses (nch_id, courthouse_name, version_number, changed_by, 
                                   changed_date, court_type, start_date, end_date, loc_loc_id, 
                                   psa_psa_id, court_location_code, sl_courthouse_name, norg_id)
VALUES
        (301, 'Duplicate Court House Entry 1', 0.0, 0, '1987-05-01', 'CHOA', '2025-01-01', NULL, 501, 1, 'DUP111', NULL, 701),
        (302, 'Duplicate Court House Entry 2', 0.0, 0, '1987-05-01', 'CHOA', '2025-01-01', NULL, 502, 1, 'DUP111', NULL, 702)
;

-- Reset the sequence for national_court_houses.nch_id;
SELECT setval('nch_seq'::regclass, (SELECT MAX(nch_id)::bigint FROM national_court_houses));

-- Create duplicate Standard_Applicants
-- STANDARD_APPLICANTS
-- Create 2 Standard Applicants that are duplicates (STANDARD_APPLICANT_CODE), with
-- STANDARD_APPLICANT_START_DATE before TODAY and STANDARD_APPLICANT_END_DATE of NULL
INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, 
                                 standard_applicant_end_date, version, changed_by, changed_date, 
                                 user_name, name, address_l1, address_l2, address_l3, address_l4, 
                                 address_l5, postcode, email_address, telephone_number)
VALUES  
    (201, 'DUP1111', DATE '2025-01-01', NULL, 0, 0, '2025-01-01 00:00:00', 'admin', 'Duplicant Applicant Code 1', '10 Applicant Road', 'Applicant Area', 'Applicant City', 'Applicant County', NULL, 'AP1 1PL', 'test1@example.com', '01111 222333'),
    (202, 'DUP1111', DATE '2025-01-01', NULL, 0, 0, '2025-01-01 00:00:00', 'admin', 'Duplicant Applicant Code 2', '20 Applicant Lane', 'Applicant District', 'Applicant Town', 'Applicant Region', NULL, 'AP2 2PL', 'test2@example.com', '04444 555666')
;

-- Reset the sequence for standard_applicants.sa_id
SELECT setval('sa_seq'::regclass, (SELECT MAX(sa_id)::bigint FROM standard_applicants));    

-- Create duplicate Resolution_Codes
-- RESOLUTION_CODES
-- Create 2 Resolution Codes that are duplicates (RESOLUTION_CODE), with
-- START_DATE before TODAY and END_DATE of NULL
INSERT INTO resolution_codes (rc_id, resolution_code, resolution_code_title, 
                              resolution_code_wording, resolution_legislation,
                              rc_destination_email_address_1, rc_destination_email_address_2, 
                              resolution_code_start_date, resolution_code_end_date,
                              version,changed_by, changed_date, user_name) VALUES
        (201, 'DUP99', 'Duplicate Entry 1', 'Duplicate Entry 1.', NULL, NULL, NULL, DATE '2025-01-01', NULL, 0, 0, TIMESTAMP '2025-01-01 00:00:00', NULL),
        (202, 'DUP99', 'Duplicate Entry 2', 'Duplicate Entry 2.', NULL, NULL, NULL, DATE '2025-01-01', NULL, 0, 0, TIMESTAMP '2025-01-01 00:00:00', NULL)
;

-- Reset the sequence for resolution_codes.rc_id
SELECT setval('rc_seq'::regclass, (SELECT MAX(rc_id)::bigint FROM resolution_codes));    

-- Create duplicate Application_Codes
-- APPLICATION_CODES
-- Create 2 Application Codes that are duplicates (APPLICATION_CODE), with
-- START_DATE before TODAY and END_DATE of NULL
INSERT INTO application_codes (ac_id, application_code, application_code_title, 
                               application_code_wording, application_legislation,fee_due, 
                               application_code_respondent, ac_destination_email_address_1, 
                               ac_destination_email_address_2, application_code_start_date, 
                               application_code_end_date, bulk_respondent_allowed, version, 
                               changed_by, changed_date, user_name, ac_fee_reference)
VALUES
        (201, 'DUP0001','Duplicate Application Code 1','Duplicate Application Code 1','',1,0,'','',DATE '2025-01-01',NULL,0, 0, 0, '2025-01-01 00:00:00', 'admin', 'CO4.1'),
        (202, 'DUP0001','Duplicate Application Code 2','Duplicate Application Code 2','',1,0,'','',DATE '2025-01-01',NULL,0, 0, 0, '2025-01-01 00:00:00', 'admin', 'CO4.1')
;

-- Reset the sequence for application_codes.ac_id
SELECT setval('ac_seq'::regclass, (SELECT MAX(ac_id)::bigint FROM application_codes));    

-- Create duplicate Application_Lists
-- APPLICATION_LISTS
-- Create 2 Application Lists that are duplicates
INSERT INTO application_lists (al_id, application_list_status, application_list_date, 
                               application_list_time, courthouse_code, other_courthouse,
                               list_description, version, changed_by, changed_date, user_name, 
                               courthouse_name, duration_hour, duration_minute, cja_cja_id)
VALUES
        (20001,'OPEN', '2025-04-21', '10:00:00', 'DUP111', null, 'Duplication Application List', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Duplicate List in Duplicate Court', 6, 3, 501),
        (20002,'OPEN', '2025-04-21', '10:00:00', 'DUP111', null, 'Duplication Application List', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Duplicate List in Duplicate Court', 6, 3, 501)
;

-- Create 1 Application Lists that will have duplicate Application_List_Entries
INSERT INTO application_lists (al_id, application_list_status, application_list_date, 
                               application_list_time, courthouse_code, other_courthouse,
                               list_description, version, changed_by, changed_date, user_name, 
                               courthouse_name, duration_hour, duration_minute, cja_cja_id)
VALUES
        (20003,'OPEN', '2025-04-21', '10:00:00', 'DUP111', null, 'Application List With Duplicate Application List Entries', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000004', 'Application List with Duplicate Applicate List Entries', 6, 3, 501)
;

-- Reset the sequence for application_lists.al_id
SELECT setval('al_seq'::regclass, (SELECT MAX(al_id)::bigint FROM application_lists));    

-- Create 2 Application List Entries that are duplicates
INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES 
    (1000, 20003, 201, 201, NULL, NULL, 0, 'Duplicate Application List Entry','AC-DUP-0001', 'DUPACC0001', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (1001, 20003, 201, 201, NULL, NULL, 0, 'Duplicate Application List Entry','AC-DUP-0001', 'DUPACC0001', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP)
;

-- Reset the sequence for application_list_entries.ale_id
SELECT setval('ale_seq'::regclass, (SELECT MAX(ale_id)::bigint FROM application_list_entries));    



-- Insert our test data for V18
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('18', 'test_support', 'check_data_expected_v18_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v18_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- check duplicate criminal_justice_areas with cja_code 'ZZ'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM criminal_justice_area WHERE
        cja_code = 'ZZ') = 2) THEN
        RAISE EXCEPTION 'Expected 2 criminal_justice_area with ZZ, got %', (SELECT COUNT(*) FROM criminal_justice_area WHERE cja_code = 'ZZ');
    END IF;

    -- check duplicate national_court_houses with court_location_code 'DUP111'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'DUP111') = 2) THEN
        RAISE EXCEPTION 'Expected 2 national_court_houses with DUP111, got %', (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'DUP111');
    END IF;

    -- check duplicate standard_applicants with standard_applicant_code 'DUP1111'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM standard_applicants WHERE
        standard_applicant_code = 'DUP1111') = 2) THEN
        RAISE EXCEPTION 'Expected 2 standard_applicants with DUP1111, got %', (SELECT COUNT(*) FROM standard_applicants WHERE standard_applicant_code = 'DUP1111');
    END IF;

    -- check duplicate resolution_codes with resolution_code 'DUP1'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM resolution_codes WHERE
        resolution_code = 'DUP99') = 2) THEN
        RAISE EXCEPTION 'Expected 2 resolution_codes with DUP99, got %', (SELECT COUNT(*) FROM resolution_codes WHERE resolution_code = 'DUP99');
    END IF; 

    -- check duplicate application_codes with application_code 'DUP0001'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_codes WHERE
        application_code = 'DUP0001') = 2) THEN
        RAISE EXCEPTION 'Expected 2 application_codes with DUP0001, got %', (SELECT COUNT(*) FROM application_codes WHERE application_code = 'DUP0001');
    END IF;

    -- check duplicate application_lists with courthouse_code 'DUP111' and list_description 'Duplication Application List'
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_lists WHERE
        courthouse_code = 'DUP111' AND list_description = 'Duplication Application List') = 2) THEN
        RAISE EXCEPTION 'Expected 2 application_lists with DUP111 and Duplication Application List, got %', (SELECT COUNT(*) FROM application_lists WHERE courthouse_code = 'DUP111' AND list_description = 'Duplication Application List');
    END IF;

    -- check duplicate application_list_entries for al_al_id = 20003
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_list_entries WHERE
        al_al_id = 20003) = 2) THEN
        RAISE EXCEPTION 'Expected 2 application_list_entries for al_al_id 20003, got %', (SELECT COUNT(*) FROM application_list_entries WHERE al_al_id = 20003);
    END IF; 

END $$;


-- Modify V14 script to reflect new data state
-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v14_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check for duplicate APPLICATION_CODES with code TEST0001
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_codes WHERE application_code = 'TEST0001') = 2) THEN
        RAISE EXCEPTION 'Expected 2 application_codes with TEST0001, got %', (SELECT COUNT(*) FROM application_codes WHERE application_code = 'TEST0001');
    END IF;

    -- Check for historical APPLICATION_CODES with end dates
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_codes WHERE application_code IN ('TEST0002','TEST0003','TEST0004','TEST0005','TEST0006','TEST0007','TEST0008','TEST0009','TEST0010') AND application_code_end_date = DATE '2025-11-06') = 9) THEN
        RAISE EXCEPTION 'Expected 9 historical application_codes with end date 2025-11-06, got %', (SELECT COUNT(*) FROM application_codes WHERE application_code IN ('TEST0002','TEST0003','TEST0004','TEST0005','TEST0006','TEST0007','TEST0008','TEST0009','TEST0010') AND application_code_end_date = DATE '2025-11-06');
    END IF;

    -- Check for duplicate RESOLUTION_CODES with code DUP1
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM resolution_codes WHERE resolution_code = 'DUP1') = 2) THEN
        RAISE EXCEPTION 'Expected 2 resolution_codes with DUP1, got %', (SELECT COUNT(*) FROM resolution_codes WHERE resolution_code = 'DUP1');
    END IF;

    -- Check for duplicate NATIONAL_COURT_HOUSES with code TEST001
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'TEST001') = 1) THEN
        RAISE EXCEPTION 'Expected 1 national_court_houses with TEST001, got %', (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'TEST001');
    END IF;

    -- Check for duplicate STANDARD_APPLICANTS with code TEST001
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM standard_applicants WHERE standard_applicant_code = 'TEST001') = 2) THEN
        RAISE EXCEPTION 'Expected 2 standard_applicants with TEST001, got %', (SELECT COUNT(*) FROM standard_applicants WHERE standard_applicant_code = 'TEST001');
    END IF;
   
    -- If all checks pass, do nothing (test passes)
END $$;

-- Modify V17 script to reflect new data state
-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v17_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- check courthouse_code is unique in national_court_houses
    IF EXISTS (
        SELECT court_location_code
        FROM national_court_houses
        WHERE nch_id < 300
        GROUP BY court_location_code
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Expected unique courthouse_code in national_court_houses, found duplicates';
    END IF;

    -- check 15 records in application_list_entries
    -- for application_list.al_id = application_list_entries.al_al_id
    -- and application_list.courthouse_code = 'TEST001'
    IF EXISTS (
        SELECT 1
        FROM application_list_entries ale
        JOIN application_lists al ON ale.al_al_id = al.al_id
        WHERE al.courthouse_code = 'TEST001'
        GROUP BY al.al_id
        HAVING COUNT(*) <> 15
    ) THEN
        RAISE EXCEPTION 'Expected 15 application_list_entries for courthouse_code TEST001, found different count';
    END IF;

    -- and for TEST002
    IF EXISTS (
        SELECT 1
        FROM application_list_entries ale
        JOIN application_lists al ON ale.al_al_id = al.al_id
        WHERE al.courthouse_code = 'TEST002'
        GROUP BY al.al_id
        HAVING COUNT(*) <> 15
    ) THEN
        RAISE EXCEPTION 'Expected 15 application_list_entries for courthouse_code TEST002, found different count';
    END IF;

END $$;

-- History
-- Matthew Harman       V1.0    07/11/2025      Test Data for ARCPOC-698
--
-- Create Two New APPLICATION_CODES with APPLICATION_CODE=TEST0001
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
        (101, 'TEST0001','Copy documents','Request to copy documents','',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1'),
        (102, 'TEST0001','Copy documents','Request to copy documents','',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1')
;

-- Nine new APPLICATION_CODES with an END_DATE in the past to test historical data handling
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
        (103, 'TEST0002','Copy documents','Request to copy documents','',1,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1'),
        (104, 'TEST0003','Copy documents (electronic)','Request for copy documents on computer disc or in electronic form','',1,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0,  '2016-01-01 00:00:00', 'admin', 'CO1.1'),
        (105, 'TEST0004','Extract from the Court Register','Certified extract from the court register','',1,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0, '2016-01-01 00:00:00','admin','CO1.1'),
        (106, 'TEST0005','Certificate of Satisfaction','Request for a certificate of satisfaction of debt registered in the register of judgements, orders and fines','',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (107, 'TEST0006','Certified genuine copy document','Request for a copy of a document certified as a genuine copy of the original document','',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (108, 'TEST0007','Appeal to Crown Court','Notice of appeal in respect of a case heard on {TEXT|Date of Hearing|10}','Section 108 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0,'2016-01-01 00:00:00','admin',NULL),
        (109, 'TEST0008','Appeal by Case Stated (Crime)','Notice of appeal to the High Court by way of case stated in respect of a criminal case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (110, 'TEST0009','Appeal by Case Stated (Civil)','Notice of appeal to the High Court by way of case stated in respect of case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (111, 'TEST0010','Certified genuine copy document','Request for a copy of a document certified as a genuine copy of the original document','',0,0,'','',DATE '2016-01-01',DATE '2025-11-06',0, 0, 0, '2016-01-01 00:00:00','admin',NULL)
;

-- Reset the sequence for application_codes.ac_id
SELECT setval('ac_seq'::regclass, (SELECT MAX(ac_id)::bigint FROM application_codes));


INSERT INTO resolution_codes (rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_code_start_date, version, changed_by, changed_date, user_name)
VALUES  
    (101, 'DUP1', 'Duplicate resolution code 1', 'This is a duplicate resolution code for testing purposes', DATE '2016-01-01', 0, 0, '2016-01-01 00:00:00', 'admin'),
    (102, 'DUP1', 'Duplicate resolution code 2', 'This is another duplicate resolution code for testing purposes', DATE '2016-01-01', 0, 0, '2016-01-01 00:00:00', 'admin')
;


-- Reset the sequence for resolution_codes.rc_id
SELECT setval('rc_seq'::regclass, (SELECT MAX(rc_id)::bigint FROM resolution_codes));   

-- Create two new NATIONAL_COURT_HOUSES with
-- the same COURT_LOCATION_CODE
INSERT INTO national_court_houses (
    nch_id,
    courthouse_name,
    version_number,
    changed_by,
    changed_date,
    court_type,
    start_date,
    end_date,
    loc_loc_id,
    psa_psa_id,
    court_location_code,
    sl_courthouse_name,
    norg_id
)
VALUES
    (201, 'Test Courthouse 1', 0, 0, '2016-01-01 00:00:00', 'MAG', '2016-01-01 00:00:00', NULL, 601, 10, 'TEST001', 'TEST01', 801),
    (202, 'Test Courthouse 2', 0, 0, '2016-01-01 00:00:00', 'MAG', '2016-01-01 00:00:00', NULL, 602, 10, 'TEST001', 'TEST01', 802)
;

-- Reset the sequence for national_court_houses.nch_id
SELECT setval('nch_seq'::regclass, (SELECT MAX(nch_id)::bigint FROM national_court_houses));    

-- Create two new STANDARD_APPLICANTS with
-- the same STANDARD_APPLICANT_CODE
INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date, version, changed_by, changed_date, user_name, name, address_l1, address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number)
VALUES  
    (101, 'TEST001', DATE '2016-01-01', NULL, 0, 0, '2016-01-01 00:00:00', 'admin', 'Test Applicant 1', '10 Applicant Road', 'Applicant Area', 'Applicant City', 'Applicant County', NULL, 'AP1 1PL', 'test1@example.com', '01111 222333'),
    (102, 'TEST001', DATE '2016-01-01', NULL, 0, 0, '2016-01-01 00:00:00', 'admin', 'Test Applicant 2', '20 Applicant Lane', 'Applicant District', 'Applicant Town', 'Applicant Region', NULL, 'AP2 2PL', 'test2@example.com', '04444 555666')
;

-- Reset the sequence for standard_applicants.sa_id
SELECT setval('sa_seq'::regclass, (SELECT MAX(sa_id)::bigint FROM standard_applicants));    


-- Insert our test data for V14
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('14', 'test_support', 'check_data_expected_v14_present')
ON CONFLICT DO NOTHING;

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
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'TEST001') = 2) THEN
        RAISE EXCEPTION 'Expected 2 national_court_houses with TEST001, got %', (SELECT COUNT(*) FROM national_court_houses WHERE court_location_code = 'TEST001');
    END IF;

    -- Check for duplicate STANDARD_APPLICANTS with code TEST001
    IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM standard_applicants WHERE standard_applicant_code = 'TEST001') = 2) THEN
        RAISE EXCEPTION 'Expected 2 standard_applicants with TEST001, got %', (SELECT COUNT(*) FROM standard_applicants WHERE standard_applicant_code = 'TEST001');
    END IF;
   
    -- If all checks pass, do nothing (test passes)
END $$;


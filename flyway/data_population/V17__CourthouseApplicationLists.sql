-- History
-- Matthew Harman       V1.0    19/11/2025      Correct Test Courthouses to have unique courthouse_code
--                                              Add 1 Application List and 15 Application List Entries to each of the 2 test courthouse
--
-- Correct Test Courthouses to have unique courthouse_code
UPDATE national_court_houses
    SET court_location_code = 'TEST002',
        sl_courthouse_name = 'TEST02'
    WHERE courthouse_name = 'Test Courthouse 2';

-- Create 1 x Application_List for each of the 2 test courthouses
----------------------- APPLICATION LIST -----------------------
INSERT INTO application_lists (
        al_id, application_list_status, application_list_date, 
        application_list_time, courthouse_code, other_courthouse,
        list_description, version, changed_by, changed_date, user_name, 
        courthouse_name, duration_hour, duration_minute, cja_cja_id
)
VALUES
        (10000,'OPEN', '2024-04-21', '10:00:00', 'TEST001', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Test Court 1 Set 1', 6, 3, NULL),
        (10001,'OPEN', '2025-04-21', '14:00:01', 'TEST002', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000002', 'Test Court 2 Set 1', 6, 3, NULL)
;

-- Reset the sequence for application_lists.al_id;
SELECT setval('al_seq'::regclass, (SELECT MAX(al_id)::bigint FROM application_lists));

-- Add 15 x Application_List_Entries for each of the 2 test courthouses
----------------------- APPLICATION LIST ENTRIES -----------------------
-- Insert 30 application_list_entries rows with changed_by as random UUID prefix before ':72f988bf-86f1-41af-91ab-2d7cd011db47'
INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    300, 10000, 101, 101, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0001', 'TESTACC001', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    301, 10000, 101, 102, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0002', 'TESTACC002', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 2, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    302, 10000, 101, 103, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0003', 'TESTACC003', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 3, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    303, 10000, 101, 104, NULL, NULL, 0, 'Request for copy documents on computer disc or in electronic form',
    'TC-001-0004', 'TESTACC004', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 4, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    304, 10000, 101, 105, NULL, NULL, 0, 'Certified extract from the court register',
    'TC-001-0005', 'TESTACC005', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 5, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    305, 10000, 101, 106, NULL, NULL, 0, 'Request for a certificate of satisfaction of debt registered in the register of judgements, orders and fines',
    'TC-001-0006', 'TESTACC006', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 6, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    306, 10000, 101, 107, NULL, NULL, 0, 'Request for a copy of a document certified as a genuine copy of the original document',
    'TC-001-0007', 'TESTACC007', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 7, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    307, 10000, 101, 108, NULL, NULL, 0, 'Notice of appeal in respect of a case heard on {23/08/2023}',
    'TC-001-0008', 'TESTACC008', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 8, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    308, 10000, 101, 109, NULL, NULL, 0, 'Notice of appeal to the High Court by way of case stated in respect of a criminal case heard on {12/05/2022}',
    'TC-001-0009', 'TESTACC009', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 9, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    309, 10000, 101, 110, NULL, NULL, 0, 'Notice of appeal to the High Court by way of case stated in respect of case heard on {01/05/2019}',
    'TC-001-0010', 'TESTACC010', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 10, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    310, 10000, 101, 111, NULL, NULL, 0, 'Request for a copy of a document certified as a genuine copy of the original document',
    'TC-001-0011', 'TESTACC011', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 11, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    311, 10000, 101, 101, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0012', 'TESTACC012', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 12, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    312, 10000, 101, 102, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0013', 'TESTACC013', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 13, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    313, 10000, 101, 103, NULL, NULL, 0, 'Request to copy documents',
    'TC-001-0014', 'TESTACC014', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 14, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    314, 10000, 101, 104, NULL, NULL, 0, 'Request for copy documents on computer disc or in electronic form',
    'TC-001-0015', 'TESTACC015', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 15, null, null, 0, CURRENT_TIMESTAMP);

-- 2nd set
INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    315, 10001, 101, 101, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0001', 'TESTACC201', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    316, 10001, 101, 102, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0002', 'TESTACC202', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 2, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    317, 10001, 101, 103, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0003', 'TESTACC203', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 3, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    318, 10001, 101, 104, NULL, NULL, 0, 'Request for copy documents on computer disc or in electronic form',
    'TC-002-0004', 'TESTACC204', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 4, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    319, 10001, 101, 105, NULL, NULL, 0, 'Certified extract from the court register',
    'TC-002-0005', 'TESTACC205', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 5, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    320, 10001, 101, 106, NULL, NULL, 0, 'Request for a certificate of satisfaction of debt registered in the register of judgements, orders and fines',
    'TC-002-0006', 'TESTACC206', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 6, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    321, 10001, 101, 107, NULL, NULL, 0, 'Request for a copy of a document certified as a genuine copy of the original document',
    'TC-002-0007', 'TESTACC207', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 7, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    322, 10001, 101, 108, NULL, NULL, 0, 'Notice of appeal in respect of a case heard on {26/09/2024}',
    'TC-002-0008', 'TESTACC208', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 8, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    323, 10001, 101, 109, NULL, NULL, 0, 'Notice of appeal to the High Court by way of case stated in respect of a criminal case heard on {11/10/2020}',
    'TC-002-0009', 'TESTACC209', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 9, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    324, 10001, 101, 110, NULL, NULL, 0, 'Notice of appeal to the High Court by way of case stated in respect of case heard on {23/04/2025}',
    'TC-002-0010', 'TESTACC210', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 10, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    325, 10001, 101, 111, NULL, NULL, 0, 'Request for a copy of a document certified as a genuine copy of the original document',
    'TC-002-0011', 'TESTACC211', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 11, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    326, 10001, 101, 101, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0012', 'TESTACC212', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 12, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    327, 10001, 101, 102, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0013', 'TESTACC213', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 13, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    328, 10001, 101, 103, NULL, NULL, 0, 'Request to copy documents',
    'TC-002-0014', 'TESTACC214', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 14, null, null, 0, CURRENT_TIMESTAMP);

INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES (
    329, 10001, 101, 104, NULL, NULL, 0, 'Request for copy documents on computer disc or in electronic form',
    'TC-002-0015', 'TESTACC215', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', 
    CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 15, null, null, 0, CURRENT_TIMESTAMP);

-- Reset the sequence for application_list_entries.ale_id;
SELECT setval('ale_seq'::regclass, (SELECT MAX(ale_id)::bigint FROM application_list_entries));

-- Create app_list_entry_Fee_id records for our data above
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    300, 222, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    301, 224, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    302, 225, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    303, 227, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    304, 229, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    305, 230, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    306, 231, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    307, 233, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    308, 235, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    309, 237, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    310, 238, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    311, 222, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    312, 224, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    313, 225, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    314, 227, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    315, 229, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    316, 230, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    317, 231, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    318, 233, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    319, 235, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    320, 237, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    321, 238, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    322, 222, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    323, 224, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    324, 225, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    325, 227, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    326, 229, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    327, 230, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    328, 231, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');
INSERT INTO app_list_entry_fee_id (ale_ale_id, fee_fee_id, version, changed_by, changed_date, user_name)
values (
    329, 233, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'TestData.Tool');


-- Insert our test data for V16
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('17', 'test_support', 'check_data_expected_v17_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v17_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- check courthouse_code is unique in national_court_houses
    IF EXISTS (
        SELECT court_location_code
        FROM national_court_houses
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

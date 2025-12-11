-- Add a name entry that is a person NOT an organisation
INSERT INTO name_address (na_id, code, name, title, forename_1, forename_2, forename_3, surname, address_l1, address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number, version, changed_by, changed_date, user_name, date_of_birth, dms_id)
VALUES
  (5, 'AP', null, 'Mr', 'John', 'Francis', 'Michael', 'Turner', '1 Market Street', NULL, NULL, NULL, NULL, 'AB11 2CD', 'john.smith@example.com', '01234567890', NULL,  0, 0, TIMESTAMP '2016-01-01 00:00:00' , NULL, NULL, NULL);

-- Add a complete list that is associated with the entry
INSERT INTO application_lists (al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse, list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id)
VALUES
  (11,'OPEN', '2024-04-21', '2024-04-21:10:00:00', 'RCJ001', 'other', 'Morning list for Family Court', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Royal Courts of Justice', 6, 3, 1);

-- add an entry with both an applicant and respondent
INSERT INTO application_list_entries (ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents, application_list_entry_wording, case_reference, entry_rescheduled, notes, version, changed_by, changed_date, bulk_upload, sequence_number, lodgement_date) VALUES
                                                                                                                                                                                                                                                                                       (10, 11, 2, 1, 5, 2, 0,'Electronic document request with 3 respondents', '232323232', 'Y', 'Rescheduled due to missing docs',1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2025-05-01 00:00:00');
-- Add a new resolution to the new entry
INSERT INTO app_list_entry_resolutions (aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording, al_entry_resolution_officer, version, changed_by, changed_date, user_name)  VALUES
                                                                                                                                                                                  (3,1,10,'Application granted in full.','Magistrate Jane Doe',1,1,CURRENT_TIMESTAMP, NULL);
-- Add a new entry as part of a deleted list
INSERT INTO application_lists (al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse, list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id, is_deleted)
VALUES
  (12,'OPEN', '2024-04-21', '2024-04-21:10:00:00', 'RCJ001', 'other', 'Morning list for Family Court', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Royal Courts of Justice', 6, 3, 1, 1);
INSERT INTO application_list_entries (ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents, application_list_entry_wording, case_reference, entry_rescheduled, notes, version, changed_by, changed_date, bulk_upload, sequence_number, lodgement_date) VALUES
  (11, 12, 2, 1, 5, 2, 0,'Electronic document request with 3 respondents', '232323232', 'Y', 'Rescheduled due to missing docs',1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2025-05-01 00:00:00');


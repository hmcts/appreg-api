INSERT INTO application_lists (al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse, list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id, is_deleted)
VALUES
  (13,'CLOSED', '2024-04-21', '2024-04-21:10:00:00', 'RCA001', 'other', 'Morning list for Family Court With No Criminal Justice Area', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Royal Courts of Justice', 6, 3, NULL, 0);

INSERT INTO application_list_entries (ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents, application_list_entry_wording, case_reference, entry_rescheduled, notes, version, changed_by, changed_date, bulk_upload, sequence_number, lodgement_date) VALUES
  (13, 13, NULL, 10, 5, 2, 0,'Electronic document request with 3 respondents', '2323232325', 'Y', 'Rescheduled due to missing docs',1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2025-05-01 00:00:00');

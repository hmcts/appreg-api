-- V83__correct_data_to_same_CSDS_shape.sql

-- Version Control
-- V1.0  	Matthew Harman  19/10/2026	Initial Version
--
-- SW99093 should have two records as per the legacy set
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, 
                               application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, 
                               application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
SELECT ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, application_code_respondent, 
       ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, 
       version, changed_by, changed_date, user_name, ac_fee_reference 
FROM application_codes_master AS m
WHERE m.ac_id = 2463
AND NOT EXISTS (
  SELECT 1
  FROM application_codes AS a
  WHERE a.ac_id = 2463
);

INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, 
                               application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, 
                               application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
SELECT ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, application_code_respondent, 
       ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, 
       version, changed_by, changed_date, user_name, ac_fee_reference 
FROM application_codes_master AS m
WHERE m.ac_id = 2442 
AND NOT EXISTS (
  SELECT 1
  FROM application_codes AS a
  WHERE a.ac_id = 2442
);


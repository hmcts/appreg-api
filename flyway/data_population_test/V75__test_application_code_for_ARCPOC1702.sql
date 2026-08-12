-- V75__test_application_code_for_ARCPOC1702.sql

-- Version Control
-- V1.0  	Matthew Harman  12/08/2026	Initial Version
--
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation, 
                               fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, 
                               application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, 
                               changed_date, user_name, ac_fee_reference) VALUES 
(20020,'MH99001','Issue of warrant of arrest in commitment proceedings - council tax (bulk)','Attends to swear a complaint for the issue of warrants of arrest for the debtors to answer an application for committal to prison (number of cases {TEXT|Number|4})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992','Y','N','','',DATE '2016-01-01',NULL,'Y',2,253290,DATE '2026-08-12','flyway.load',NULL)
;


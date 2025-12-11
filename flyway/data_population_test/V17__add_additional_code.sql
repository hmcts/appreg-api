-- Add a code that requires a respondent fee of 1 unit for testing purposes and can be bulk respondent
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
  (43, 'MS99007','Copy documents','Application for a warrant to enter premises at {TEXT|Premises Address|15} for date {DATE|Premises Date|10}','',1,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,1, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1'),
  (44, 'ZS99007','Copy documents','Application for a warrant to enter premises at {TEXT|Premises Address|20} for date {DATE|Premises Date|10}','',1,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO2.4');

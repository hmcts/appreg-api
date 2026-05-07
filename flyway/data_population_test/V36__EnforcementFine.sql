INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
  (45, 'EF1213','Enforcement Fine',
   'This is a test enforcement fine with no wording template substitution required',
   '','Y','Y','',
   '',TIMESTAMP '2016-01-01 00:00:00',NULL,
   'Y', 0, 0, '2016-01-01 00:00:00', 'admin',
   'CO1.1');

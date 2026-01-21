-- application_codes --
update application_codes set fee_due = 'Y' where fee_due = '1';
update application_codes set fee_due = 'N' where fee_due = '0';
update application_codes set application_code_respondent = 'Y' where application_code_respondent = '1';
update application_codes set application_code_respondent = 'N' where application_code_respondent = '0';
update application_codes set bulk_respondent_allowed = 'Y' where bulk_respondent_allowed = '1';
update application_codes set bulk_respondent_allowed = 'N' where bulk_respondent_allowed = '0';

-- application_list_entries --
update application_list_entries set entry_rescheduled = 'Y' where entry_rescheduled = '1';
update application_list_entries set entry_rescheduled = 'N' where entry_rescheduled = '0';
update application_list_entries set bulk_upload = 'Y' where bulk_upload = '1';
update application_list_entries set bulk_upload = 'N' where bulk_upload = '0';

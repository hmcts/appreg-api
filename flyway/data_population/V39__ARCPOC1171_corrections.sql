-- V39__ARCPOC1171_corrections.sql
--
-- Correct erronous APPLICATION_CODES entries that have spaces in the { } section

UPDATE application_codes
SET application_code_wording = 'Application to transfer enforcement of the financial penalty to {TEXT|LJA Name|120}'
WHERE application_code = 'EF99004';

UPDATE application_codes
SET application_code_wording = 'Application to transfer enforcement of the financial penalty to Scottish court: {TEXT|Scottish Court Name|120}'
WHERE application_code = 'EF99005';

UPDATE application_codes
SET application_code_wording = 'Application to transfer enforcement of the financial penalty to Northern Ireland court: {TEXT|N Irish Court Name|120}'
WHERE application_code = 'EF99006';

UPDATE application_codes
SET application_code_wording = 'Application for the issue of an arrest warrant (without bail) to attend court for means enquiry to enforce the remaining balance of {TEXT|Account balance|10}'
WHERE application_code = 'EF99008';

UPDATE application_codes
SET application_code_wording = 'Application for the issue of an arrest warrant (with bail) to attend court for means enquiry to enforce the remaining balance of {TEXT|Account balance|10}'
WHERE application_code = 'EF99009';



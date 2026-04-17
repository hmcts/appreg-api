-- V45__correct_application_codes_ARCPOC_1310.sql

-- Version Control
-- V1.0  	Matthew Harman  16/04/2026	Initial Version
--

UPDATE application_codes
SET application_code_wording = 'Application for a warrant to authorise the use of reasonable force to execute a warrant of control. Balance: {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last Payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}'
WHERE application_code = 'EF99012';

UPDATE application_codes
SET application_code_wording = 'Application for a warrant to authorise the use of reasonable force to  take control of goods on a highway. Balance {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date_imposed|10} Last payment: {DT|3_Date_last payment|10} Last enforcement: {TXT|4_Last enforcement|10}'
WHERE application_code = 'EF99013';

UPDATE application_codes
SET application_code_wording = 'Application for an order to specifying less than 7 days as the period of time for giving notice of enforcement prior to taking control of goods. Balance: {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}'
WHERE application_code = 'EF99015';

UPDATE application_codes
SET application_code_wording = 'Application for the endorsement of process issued in Scotland, namely {TEXT|1_State nature of process order or warrant|250}'
WHERE application_code = 'MX99014';

UPDATE application_codes
SET application_code_wording = 'Application by {TEXT|Applicants name|70} for an order covering {TEXT|No. of accounts|3} account(s), by requiring the respondent to either produce or allow access to material that is in their possession likely to be of substantial value to the investigation'
WHERE application_code = 'SW99033';

UPDATE application_codes
SET application_code_wording = 'Application by {TEXT|Applicants name|70} for an order covering {TEXT|No. of accounts|3} accounts(s), requiring the respondent to either produce or allow access to material that is in their possession for the purpose of an investigation into drug trafficking'
WHERE application_code = 'SW99034';

UPDATE application_codes
SET application_code_wording = 'Application {TEXT|Applicants name|70} for a production order of covering {TEXT|No. of accounts|3} account(s), by requiring the respondent to either produce or allow access to material that is in their possession or control for the purpose of a relevant investigation under the Act with an order to enter {TEXT|Premises Address|200} to obtain access to the material'
WHERE application_code = 'SW99094';

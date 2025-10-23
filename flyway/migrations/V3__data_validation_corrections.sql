-- Alter tables after comparison with Oracle 

-- Version Control
-- V1.0  	Matthew Harman  29/08/2025	Initial Version
-- V2.0  	Matthew Harman  16/09/2025	Added code for testing script applied
-- V3.0		Matthew Harman	26/09/2025	Added UUID fields to APPLICATION_LISTS and APPLICATION_LIST_ENTRIES
-- V4.0 	Matthew Harman	06/10/2025	ARCPOC-602 - Amended Changed_By fields on
--				                                      APP_LIST_ENTRY_FEE_ID
--					                                  APP_LIST_ENTRY_FEE_STATUS
--					                                  APPLICATION_LIST_ENTRIES
--					                                  APP_LIST_ENTRY_RESOLUTIONS
--					                                  APP_LIST_ENTRY_OFFICIAL
--					                                  APPLICATION_LISTS
--					                                  APPLICATION_REGISTER
-- V5.0 	Matthew Harman	13/10/2025	ARCPOC-620 - Changed NAME_ADDRESS Changed_By fileds
-- V6.0 	Matthew Harman	13/10/2025	ARCPOC-619 - Changed APPLICATION_LISTS:
--														APPLICATION_LIST_DATE to DATE
--														APPLICATION_LIST_TIME to TIME
-- V7.0 	Matthew Harman	20/10/2025	ARCPOC-621 - Changed USER_ID on DATA_AUDIT
-- V8.0	    Matthew Harman	22/10/2025	ARCPOC-647 - Change to type DATE the following fields:
--													APPLICATION_CODES.APPLICATION_CODE_START_DATE
--													APPLICATION_CODES.APPLICATION_CODE_END_DATE
--													STANDARD_APPLICANTS.STANDARD_APPLICANT_START_DATE
--													STANDARD_APPLICANTS.STANDARD_APPLICANT_END_DATE
--												    FEE.FEE_START_DATE	 
--												    FEE.FEE_END_DATE
--													RESOLUTION_CODES.RESOLUTION_CODE_START_DATE
--													RESOLUTION_CODES.RESOLUTION_CODE_END_DATE
--													PETTY_SESSIONAL_AREAS.START_DATE
--													PETTY_SESSIONAL_AREAS.END_DATE
--													NATIONAL_COURT_HOUSES.START_DATE
--													NATIONAL_COURT_HOUSES.END_DATE	
--													ADDRESSES.START_DATE
--													ADDRESSES.END_DATE
--													COMMUNICATION_MEDIA.START_DATE
--													COMMUNICATION_MEDIA.END_DATE
--													LINK_COMMUNICATION_MEDIA.START_DATE
--													LINK_COMMUNICATION_MEDIA.END_DATE
--													LINK_ADDRESSES.START_DATE
--													LINK_ADDRESSES.END_DATE



SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

ALTER TABLE FEE ALTER COLUMN FEE_VALUE TYPE NUMERIC(9,2);

DROP SEQUENCE IF EXISTS ale_seq;
CREATE SEQUENCE ale_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2975601 CACHE 20;

DROP SEQUENCE IF EXISTS nch_seq;
CREATE SEQUENCE nch_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 9230 CACHE 20;

-- Add uuid fields to APPLICATION_LISTS and APPLICATION_LIST_ENTRIES
ALTER TABLE application_lists ADD COLUMN id UUID DEFAULT gen_random_uuid() UNIQUE;
ALTER TABLE application_list_entries ADD COLUMN id UUID DEFAULT gen_random_uuid() UNIQUE;

-- Modify the changed_date fields to be VARCHAR(73) NOT NULL
ALTER TABLE app_list_entry_fee_id ALTER COLUMN changed_by TYPE VARCHAR(73);
ALTER TABLE app_list_entry_fee_status ALTER COLUMN alefs_changed_by TYPE VARCHAR(73);
ALTER TABLE application_list_entries ALTER COLUMN changed_by TYPE VARCHAR(73);
ALTER TABLE app_list_entry_resolutions ALTER COLUMN changed_by TYPE VARCHAR(73);	
ALTER TABLE app_list_entry_official ALTER COLUMN changed_by TYPE VARCHAR(73);
ALTER TABLE application_lists ALTER COLUMN changed_by TYPE VARCHAR(73);
ALTER TABLE application_register ALTER COLUMN changed_by TYPE VARCHAR(73);

-- ARCPOC-620 - Change NAME_ADDRESS Changed_By fields to VARCHAR(73)
ALTER TABLE name_address ALTER COLUMN changed_by TYPE VARCHAR(73);

-- ARCPOC-621 - Change USER_ID on DATA_AUDIT to VARCHAR(73)
ALTER TABLE data_audit ALTER COLUMN user_id TYPE VARCHAR(73);

-- ARCPOC-619 - Change APPLICATION_LISTS:
-- APPLICATION_LIST_DATE to DATE		
-- APPLICATION_LIST_TIME to TIME
ALTER TABLE application_lists ALTER COLUMN application_list_date TYPE DATE;
ALTER TABLE application_lists ALTER COLUMN application_list_time TYPE TIME;	

-- ARCPOC-647
-- Change to type DATE the following fields:
--	APPLICATION_CODES.APPLICATION_CODE_START_DATE
--	APPLICATION_CODES.APPLICATION_CODE_END_DATE
--	STANDARD_APPLICANTS.STANDARD_APPLICANT_START_DATE
--	STANDARD_APPLICANTS.STANDARD_APPLICANT_END_DATE
--  FEE.FEE_START_DATE	 
--  FEE.FEE_END_DATE
--	RESOLUTION_CODES.RESOLUTION_CODE_START_DATE
--	RESOLUTION_CODES.RESOLUTION_CODE_END_DATE
--	PETTY_SESSIONAL_AREAS.START_DATE
--  PETTY_SESSIONAL_AREAS.END_DATE
--	NATIONAL_COURT_HOUSES.START_DATE
--	NATIONAL_COURT_HOUSES.END_DATE	
--	ADDRESSES.START_DATE
--	ADDRESSES.END_DATE
--	COMMUNICATION_MEDIA.START_DATE
--	COMMUNICATION_MEDIA.END_DATE
--	LINK_COMMUNICATION_MEDIA.START_DATE
--	LINK_COMMUNICATION_MEDIA.END_DATE
--	LINK_ADDRESSES.START_DATE
--	LINK_ADDRESSES.END_DATE

-- Drop views that depend on these columns
DROP VIEW IF EXISTS v_application_codes_noclob;
DROP VIEW IF EXISTS v_resolution_codes_noclob;

ALTER TABLE application_codes ALTER COLUMN application_code_start_date TYPE DATE;
ALTER TABLE application_codes ALTER COLUMN application_code_end_date TYPE DATE;	
ALTER TABLE standard_applicants ALTER COLUMN standard_applicant_start_date TYPE DATE;
ALTER TABLE standard_applicants ALTER COLUMN standard_applicant_end_date TYPE DATE;
ALTER TABLE fee ALTER COLUMN fee_start_date TYPE DATE;
ALTER TABLE fee ALTER COLUMN fee_end_date TYPE DATE;	
ALTER TABLE resolution_codes ALTER COLUMN resolution_code_start_date TYPE DATE;
ALTER TABLE resolution_codes ALTER COLUMN resolution_code_end_date TYPE DATE;
ALTER TABLE petty_sessional_areas ALTER COLUMN start_date TYPE DATE;
ALTER TABLE petty_sessional_areas ALTER COLUMN end_date TYPE DATE;	
ALTER TABLE national_court_houses ALTER COLUMN start_date TYPE DATE;
ALTER TABLE national_court_houses ALTER COLUMN end_date TYPE DATE;
ALTER TABLE addresses ALTER COLUMN start_date TYPE DATE;
ALTER TABLE addresses ALTER COLUMN end_date TYPE DATE;	
ALTER TABLE communication_media ALTER COLUMN start_date TYPE DATE;
ALTER TABLE communication_media ALTER COLUMN end_date TYPE DATE;
ALTER TABLE link_communication_media ALTER COLUMN start_date TYPE DATE;
ALTER TABLE link_communication_media ALTER COLUMN end_date TYPE DATE;
ALTER TABLE link_addresses ALTER COLUMN start_date TYPE DATE;
ALTER TABLE link_addresses ALTER COLUMN end_date TYPE DATE;

-- Recreate views
CREATE OR REPLACE VIEW v_application_codes_noclob (ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference) AS SELECT AC_ID
,      APPLICATION_CODE
,      APPLICATION_CODE_TITLE
,      substr(APPLICATION_CODE_WORDING,4000,1) application_code_wording
,      substr(APPLICATION_LEGISLATION,4000,1)  application_legislation
,      FEE_DUE
,      APPLICATION_CODE_RESPONDENT
,      AC_DESTINATION_EMAIL_ADDRESS_1
,      AC_DESTINATION_EMAIL_ADDRESS_2
,      APPLICATION_CODE_START_DATE
,      APPLICATION_CODE_END_DATE
,      BULK_RESPONDENT_ALLOWED
,      VERSION
,      CHANGED_BY
,      CHANGED_DATE
,      USER_NAME
,      AC_FEE_REFERENCE
FROM   application_codes;

CREATE OR REPLACE VIEW v_resolution_codes_noclob (rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_legislation, rc_destination_email_address_1, rc_destination_email_address_2, resolution_code_start_date, resolution_code_end_date, version, changed_by, changed_date, user_name) AS SELECT RC_ID
,      RESOLUTION_CODE
,      RESOLUTION_CODE_TITLE
,      substr(RESOLUTION_CODE_WORDING,4000,1)  RESOLUTION_CODE_WORDING
,      substr(RESOLUTION_LEGISLATION,4000,1)   RESOLUTION_LEGISLATION
,      RC_DESTINATION_EMAIL_ADDRESS_1
,      RC_DESTINATION_EMAIL_ADDRESS_2
,      RESOLUTION_CODE_START_DATE
,      RESOLUTION_CODE_END_DATE
,      VERSION
,      CHANGED_BY
,      CHANGED_DATE
,      USER_NAME
FROM   resolution_codes;

-- Insert our test data for V3
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('3', 'test_support', 'check_schema_objects_v3_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v3_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check for existence of fee.fee_value as a numeric field
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fee' and column_name = 'fee_value' and data_type = 'numeric') THEN
		RAISE EXCEPTION 'Table: fee  Column: fee_value is not a numeric field';
	END IF;

	-- Check for existence of fee.fee_value as a numeric field
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_lists' and column_name = 'id') THEN
		RAISE EXCEPTION 'Table: application_lists  Column: id is missing';
	END IF;

	-- Check for existence of fee.fee_value as a numeric field
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_list_entries' and column_name = 'id') THEN
		RAISE EXCEPTION 'Table: application_list_entries  Column: id is missing';
	END IF;

	-- Check app_list_entry_fee_id.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_list_entry_fee_id' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: app_list_entry_fee_id  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check app_list_entry_fee_status.alefs_changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_list_entry_fee_status' and column_name = 'alefs_changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: app_list_entry_fee_status  Column: alefs_changed_by is not a varchar(73)';
	END IF;

	-- Check application_list_entries.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_list_entries' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: application_list_entries  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check app_list_entry_resolutions.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_list_entry_resolutions' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: app_list_entry_resolutions  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check app_list_entry_official.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_list_entry_official' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: app_list_entry_official  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check application_lists.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_lists' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: application_lists  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check application_register.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_register' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: application_register  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check name_address.changed_by is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'name_address' and column_name = 'changed_by' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: name_address  Column: changed_by is not a varchar(73)';
	END IF;

	-- Check application_lists.application_list_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_lists' and column_name = 'application_list_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: application_lists  Column: application_list_date is not a date field';
	END IF;

	-- Check application_lists.application_list_time is TIME
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_lists' and column_name = 'application_list_time' and data_type = 'time without time zone') THEN
		RAISE EXCEPTION 'Table: application_lists  Column: application_list_time is not a time field';
	END IF;

	-- Check data_audit.user_id is varchar(73)
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'data_audit' and column_name = 'user_id' and data_type = 'character varying' and character_maximum_length = 73) THEN
		RAISE EXCEPTION 'Table: data_audit  Column: user_id is not a varchar(73)';
	END IF;

	-- Check application_codes.application_code_start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_codes' and column_name = 'application_code_start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: application_codes  Column: application_code_start_date is not a date field';
	END IF;

	-- Check application_codes.application_code_end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'application_codes' and column_name = 'application_code_end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: application_codes  Column: application_code_end_date is not a date field';
	END IF;

	-- Check standard_applicants.standard_applicant_start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'standard_applicants' and column_name = 'standard_applicant_start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: standard_applicants  Column: standard_applicant_start_date is not a date field';
	END IF;

	-- Check standard_applicants.standard_applicant_end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'standard_applicants' and column_name = 'standard_applicant_end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: standard_applicants  Column: standard_applicant_end_date is not a date field';
	END IF;

	-- Check fee.fee_start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fee' and column_name = 'fee_start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: fee  Column: fee_start_date is not a date field';
	END IF;

	-- Check fee.fee_end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fee' and column_name = 'fee_end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: fee  Column: fee_end_date is not a date field';
	END IF;

	-- Check resolution_codes.resolution_code_start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'resolution_codes' and column_name = 'resolution_code_start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: resolution_codes  Column: resolution_code_start_date is not a date field';
	END IF;

	-- Check resolution_codes.resolution_code_end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'resolution_codes' and column_name = 'resolution_code_end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: resolution_codes  Column: resolution_code_end_date is not a date field';
	END IF;

	-- Check petty_sessional_areas.start_date is DATE	
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'petty_sessional_areas' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: petty_sessional_areas  Column: start_date is not a date field';
	END IF;

	-- Check petty_sessional_areas.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'petty_sessional_areas' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: petty_sessional_areas  Column: end_date is not a date field';
	END IF;

	-- Check national_court_houses.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'national_court_houses' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: national_court_houses  Column: start_date is not a date field';
	END IF;

	-- Check national_court_houses.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'national_court_houses' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: national_court_houses  Column: end_date is not a date field';
	END IF;

	-- Check addresses.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'addresses' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: addresses  Column: start_date is not a date field';
	END IF;

	-- Check addresses.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'addresses' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: addresses  Column: end_date is not a date field';
	END IF;

	-- Check communication_media.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'communication_media' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: communication_media  Column: start_date is not a date field';
	END IF;

	-- Check communication_media.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'communication_media' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: communication_media  Column: end_date is not a date field';
	END IF;

	-- Check link_communication_media.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'link_communication_media' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: link_communication_media  Column: start_date is not a date field';
	END IF;

	-- Check link_communication_media.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'link_communication_media' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: link_communication_media  Column: end_date is not a date field';
	END IF;

	-- Check link_addresses.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'link_addresses' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: link_addresses  Column: start_date is not a date field';
	END IF;	
	
	-- Check link_addresses.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'link_addresses' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: link_addresses  Column: end_date is not a date field';
	END IF;	
	
	-- If all checks pass, do nothing (test passes)
END $$;

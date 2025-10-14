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

-- ARCPOC-619 - Change APPLICATION_LISTS:
-- APPLICATION_LIST_DATE to DATE		
-- APPLICATION_LIST_TIME to TIME
ALTER TABLE application_lists ALTER COLUMN application_list_date TYPE DATE;
ALTER TABLE application_lists ALTER COLUMN application_list_time TYPE TIME;	

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

	-- If all checks pass, do nothing (test passes)
END $$;
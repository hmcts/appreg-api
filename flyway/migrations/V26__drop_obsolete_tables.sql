-- Remove obsolete tables

-- Version Control
-- V1.0  	Matthew Harman      15/01/2026	Initial version
--

-- Addresses table is not used, created in error, drop it
DROP TABLE addresses;

DROP SEQUENCE IF EXISTS adr_seq;

-- Link_Communication_Media table is not used, created in error, drop it
DROP TABLE link_communication_media;

DROP SEQUENCE IF EXISTS lcm_seq;

-- Communication_Media table is not used, created in error, drop it
DROP TABLE communication_media;

DROP SEQUENCE IF EXISTS comm_seq;

-- Link_Addresses table is not used, created in error, drop it
DROP TABLE link_addresses;

DROP SEQUENCE IF EXISTS la_seq;

-- Petty_Sessional_Areas table is not used, created in error, drop it
-- drop the constraint first
ALTER TABLE national_court_houses DROP CONSTRAINT nch_psa_fk;

DROP TABLE petty_sessional_areas;

DROP SEQUENCE IF EXISTS psa_seq;

-- Insert our test data for V26
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('26', 'test_support', 'check_schema_objects_v26_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v26_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check addresses table is dropped
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'addresses') THEN
            RAISE EXCEPTION 'Table: addresses still exists';
        END IF;

        -- Check communication_media table is dropped
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'communication_media') THEN
            RAISE EXCEPTION 'Table: communication_media still exists';
        END IF;

        -- Check link_communication_media table is dropped
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'link_communication_media') THEN
            RAISE EXCEPTION 'Table: link_communication_media still exists';
        END IF;

        -- Check link_addresses table is dropped
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'link_addresses') THEN
            RAISE EXCEPTION 'Table: link_addresses still exists';
        END IF;

        -- Check petty_sessional_areas table is dropped
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'petty_sessional_areas') THEN
            RAISE EXCEPTION 'Table: petty_sessional_areas still exists';
        END IF;
    END $$;

-- Insert our test data for V3
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('3', 'test_support', 'check_schema_objects_v3_present')
ON CONFLICT DO NOTHING;

-- Modify V3 script based on corrections becuase of running V26
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

	-- Check national_court_houses.start_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'national_court_houses' and column_name = 'start_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: national_court_houses  Column: start_date is not a date field';
	END IF;

	-- Check national_court_houses.end_date is DATE
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'national_court_houses' and column_name = 'end_date' and data_type = 'date') THEN
		RAISE EXCEPTION 'Table: national_court_houses  Column: end_date is not a date field';
	END IF;

	
	-- If all checks pass, do nothing (test passes)
END $$;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v1_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'application_codes') THEN
		RAISE EXCEPTION 'Table application_codes is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'application_lists') THEN
		RAISE EXCEPTION 'Table application_lists is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'application_list_entries') THEN
		RAISE EXCEPTION 'Table application_list_entries is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'application_register') THEN
		RAISE EXCEPTION 'Table application_register is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_list_entry_fee_id') THEN
		RAISE EXCEPTION 'Table app_list_entry_fee_id is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_list_entry_fee_status') THEN
		RAISE EXCEPTION 'Table app_list_entry_fee_status is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_list_entry_official') THEN
		RAISE EXCEPTION 'Table app_list_entry_official is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'app_list_entry_resolutions') THEN
		RAISE EXCEPTION 'Table app_list_entry_resolutions is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'criminal_justice_area') THEN
		RAISE EXCEPTION 'Table criminal_justice_area is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'fee') THEN
		RAISE EXCEPTION 'Table fee is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'name_address') THEN
		RAISE EXCEPTION 'Table name_address is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'national_court_houses') THEN
		RAISE EXCEPTION 'Table national_court_houses is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'resolution_codes') THEN
		RAISE EXCEPTION 'Table resolution_codes is missing';
	END IF;
	IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'standard_applicants') THEN
		RAISE EXCEPTION 'Table standard_applicants is missing';
	END IF;
	-- If all checks pass, do nothing (test passes)
END $$;

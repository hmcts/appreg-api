-- V53__reconfigure_database_job_code.sql

-- Version Control
-- V1.0  	Matthew Harman  29/05/2026	Initial Version
--

-- Modify the existing tables
-- Drop the retention_policy_configuration table if it exists
DROP TABLE IF EXISTS retention_policy_configuration;

-- Drop the sequence RPC_SEQ if it exists
DROP SEQUENCE IF EXISTS rpc_seq;

-- Modify the rentention_policy table to remove retention_policy_start_date,retention_policy_end_date,
-- retention_policy_metadata columns
ALTER TABLE retention_policy
    DROP COLUMN IF EXISTS retention_policy_start_date,
    DROP COLUMN IF EXISTS retention_policy_end_date,
    DROP COLUMN IF EXISTS retention_policy_metadata;

-- Add additional columns to the retention_policy table
-- config_key character varying(255)
-- config_value character varying(255)
-- config_notes character varying(1000)
ALTER TABLE retention_policy
    ADD COLUMN IF NOT EXISTS config_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS config_value VARCHAR(255),
    ADD COLUMN IF NOT EXISTS config_notes VARCHAR(1000); 

-- Add the column parameters_used character varying(500) to the database_job_execution_log table
ALTER TABLE database_job_execution_log
    ADD COLUMN IF NOT EXISTS parameters_used VARCHAR(500);

-- Drop the function get_retention_policy_id
DROP FUNCTION IF EXISTS get_retention_policy_id;

-- Drop the function get_retention_policy_parameter
DROP FUNCTION IF EXISTS get_retention_policy_parameter;

-- Create the new version of get_retention_policy_parameter function
CREATE OR REPLACE FUNCTION get_retention_policy_parameter(
	p_parameter_name text,
	p_job_name text)
    RETURNS text
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE 
	p_policy_value text;
BEGIN 
	SELECT rp.config_value 
	    INTO p_policy_value
		FROM ${flyway:defaultSchema}.retention_policy rp, ${flyway:defaultSchema}.database_jobs dj
		WHERE rp.dj_dj_id = dj.dj_id
		AND dj.job_name = p_job_name
		AND rp.config_key = p_parameter_name;

	IF NOT FOUND THEN
		RAISE EXCEPTION USING
			ERRCODE = 'JO001', 
			MESSAGE = format('job "%s" does not have a value for %s', p_job_name, p_parameter_name); 
	END IF;

	RETURN p_policy_value;
END; 
$BODY$;

-- Create the new function delete_expired_application_lists_core
CREATE OR REPLACE FUNCTION delete_expired_application_lists_core(
	p_return_count boolean DEFAULT false)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
	l_job_enabled BOOLEAN;
	l_execution_start_time TIMESTAMP;
	l_database_job TEXT;
	l_error_string TEXT;
	l_result_string TEXT;
	l_rp_id	BIGINT;
	l_retention_period TEXT;
	l_policy_parameter TEXT;
	l_retention_period_value INTEGER;
	l_number_evaluated INTEGER;
	l_enable_data_audit TEXT;
	l_data_audit BOOLEAN;
	l_success_counter INTEGER;
	l_parameters_used TEXT;
	
	cur_application_lists CURSOR (p_retention integer) FOR
		SELECT al_id
		    FROM ${flyway:defaultSchema}.application_lists
		    WHERE application_list_status = 'CLOSED'
		    AND application_list_date < now() - make_interval(days => p_retention)
		    AND NOT child_deleted;
	r_application_list RECORD;

	cur_app_list_entry_resolutions CURSOR (p_al_id integer) FOR
		SELECT *
		    FROM ${flyway:defaultSchema}.app_list_entry_resolutions
		    WHERE ale_ale_id IN (SELECT ale_id
							        FROM ${flyway:defaultSchema}.application_list_entries
							        WHERE al_al_id = p_al_id);
	r_app_list_entry_resolutions RECORD;

	cur_app_list_entry_fee_id CURSOR (p_al_id integer) FOR
		SELECT *
		    FROM ${flyway:defaultSchema}.app_list_entry_fee_id
		    WHERE ale_ale_id IN (SELECT ale_id
							    FROM ${flyway:defaultSchema}.application_list_entries
							    WHERE al_al_id = p_al_id);
	r_app_list_entry_fee_id RECORD;

	cur_app_list_entry_fee_status CURSOR (p_al_id integer) FOR
		SELECT *
		FROM ${flyway:defaultSchema}.app_list_entry_fee_status
		WHERE alefs_ale_id IN (SELECT ale_id
							   FROM ${flyway:defaultSchema}.application_list_entries
							   WHERE al_al_id = p_al_id);
	r_app_list_entry_fee_status RECORD;

	cur_app_list_entry_official CURSOR (p_al_id integer) FOR
		SELECT *
		FROM ${flyway:defaultSchema}.app_list_entry_official
		WHERE ale_ale_id IN (SELECT ale_id
							   FROM ${flyway:defaultSchema}.application_list_entries
							   WHERE al_al_id = p_al_id);
	r_app_list_entry_official RECORD;

	cur_al_ale_sequence_mapping CURSOR (p_al_id integer) FOR
		SELECT *
		FROM ${flyway:defaultSchema}.al_ale_sequence_mapping
		WHERE al_id = p_al_id;
	r_al_ale_sequence_mapping RECORD;

	cur_application_register CURSOR (p_al_id integer) FOR
		SELECT *
		FROM ${flyway:defaultSchema}.application_register
		WHERE al_al_id = p_al_id;
	r_application_register RECORD;

	cur_application_list_entries CURSOR (p_al_id integer) FOR
		SELECT *
		FROM ${flyway:defaultSchema}.application_list_entries
		WHERE al_al_id = p_al_id;
	r_application_list_entries RECORD;
BEGIN
	l_parameters_used = '';
	BEGIN
		l_database_job = 'APPLICATION_LISTS_DATABASE_JOB';
		l_success_counter = 0;
	
		-- get the execution start_time
		l_execution_start_time = NOW();
    	-- check if the database job has been created
		BEGIN
			PERFORM ${flyway:defaultSchema}.is_database_job_created(l_database_job);
		EXCEPTION
			WHEN SQLSTATE 'JC001' THEN
				l_error_string = concat('Job ',l_database_job,' has multiple records in the database_jobs table');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job::text,
						l_execution_start_time::timestamp,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
			WHEN SQLSTATE 'JC002' THEN
				l_error_string = concat('Job ',l_database_job,' does not have a record in the database_jobs table');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
		END;	

		-- check job is enabled
    	BEGIN
			PERFORM ${flyway:defaultSchema}.is_database_job_enabled(l_database_job);
		EXCEPTION
			WHEN SQLSTATE 'JE001' THEN
				l_error_string = concat('Job ',l_database_job,' does not exist');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
			WHEN SQLSTATE 'JE002' THEN
				l_error_string = concat('Job ',l_database_job,' is created multiple times');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
			WHEN SQLSTATE 'JE003' THEN
				l_error_string = concat('Job ',l_database_job,' is in indeterminable state');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
			WHEN SQLSTATE 'JE004' THEN
				l_error_string = concat('Job ',l_database_job,' is not enabled');
	
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
		END;	

		-- get the retention period
		l_policy_parameter = 'RETENTION_PERIOD_DAYS';
		BEGIN
			SELECT ${flyway:defaultSchema}.get_retention_policy_parameter(l_policy_parameter,l_database_job) INTO l_retention_period;
			l_parameters_used = concat(l_parameters_used,'#',l_policy_parameter,':',l_retention_period,'#');
		EXCEPTION
			WHEN SQLSTATE 'JO001' THEN
				l_error_string = concat('Job ',l_database_job,' does not have policy parameter ',l_policy_parameter);
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
		END;

		-- convert the retention period into an integer
		BEGIN
			SELECT CAST(l_retention_period AS INTEGER) INTO l_retention_period_value;
		EXCEPTION
			WHEN OTHERS THEN
				l_error_string = concat('Job ',l_database_job,' retention period is not a number of days');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
		END;
	
		-- get the retention period
		l_policy_parameter = 'ENABLE_DATA_AUDIT';
		BEGIN
			SELECT ${flyway:defaultSchema}.get_retention_policy_parameter(l_policy_parameter,l_database_job) INTO l_enable_data_audit;
			l_parameters_used = concat(l_parameters_used,'#',l_policy_parameter,':',l_enable_data_audit,'#');
		EXCEPTION
			WHEN SQLSTATE 'JO001' THEN
				l_error_string = concat('Job ',l_database_job,' does not have policy parameter ',l_policy_parameter);
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
		END;

		-- check enable_data_audit is Y|y|N|n|TRUE|FALSE|true|false
		BEGIN
			IF l_enable_data_audit IN ('Y','y','TRUE','true') THEN
				l_data_audit = TRUE;
			ELSIF l_enable_data_audit IN ('N','n','FALSE','false') THEN
				l_data_audit = FALSE;
			ELSE
				l_error_string = concat('Job ',l_database_job,' enable_data_audit is not a value of Y|y|N|n|TRUE|FALSE|true|false');
				-- only write to the job status table if not doing a dry run
				IF NOT p_return_count THEN
					CALL ${flyway:defaultSchema}.write_job_status_table(
						l_database_job,
						l_execution_start_time,
						now()::timestamp,
						'SKIPPED',
						0,
						0,
						l_error_string,
						l_parameters_used);
				END IF;
				IF p_return_count THEN
					RETURN 0;
				END IF;

				RETURN NULL;
			END IF;
		END;		

		-- if we get here we can start to run the actual deletions
		-- find out how many lists we are going to evaluate
		SELECT count(*) INTO l_number_evaluated
		FROM ${flyway:defaultSchema}.application_lists
		WHERE application_list_status = 'CLOSED'
		AND application_list_date < now() - make_interval(days => l_retention_period_value)
		AND NOT child_deleted;

		IF l_number_evaluated > 0 THEN
			-- loop through them and do the deletes if applicable
			OPEN cur_application_lists(l_retention_period_value);
			LOOP
				FETCH cur_application_lists INTO r_application_list;
				EXIT WHEN NOT FOUND;
	
				-- Delete from APP_LIST_ENTRY_RESOLUTIONS
				OPEN cur_app_list_entry_resolutions(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_resolutions INTO r_app_list_entry_resolutions;
					EXIT WHEN NOT FOUND;

					-- If we are doing a dry run don't audit
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'aler_id'::text,
														r_app_list_entry_resolutions.aler_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
    													'N'::text);
	
	    					CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'rc_rc_id'::text,
														r_app_list_entry_resolutions.rc_rc_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
							
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'ale_ale_id'::text,
														r_app_list_entry_resolutions.ale_ale_id::text,
														l_database_job::text,
													    'Scheduled Job'::text,
														'N'::text);
															
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'al_entry_resolution_wording'::text,
														r_app_list_entry_resolutions.al_entry_resolution_wording::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'Y'::text);
																
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'al_entry_resolution_officer'::text,
														r_app_list_entry_resolutions.al_entry_resolution_officer::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
																
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'version'::text,
														r_app_list_entry_resolutions.version::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
																
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'changed_by'::text,
														r_app_list_entry_resolutions.changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
																
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'changed_date'::text,
														r_app_list_entry_resolutions.changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
																
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'user_name'::text,
														r_app_list_entry_resolutions.user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
															
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_resolutions'::text,
														'id'::text,
														r_app_list_entry_resolutions.id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;

					-- DELETE the record only if not a dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.app_list_entry_resolutions 
                    	WHERE aler_id = r_app_list_entry_resolutions.aler_id;
					END IF;
				END LOOP;
				CLOSE cur_app_list_entry_resolutions;

				-- Move onto app_list_entry_fee_id
				OPEN cur_app_list_entry_fee_id(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_fee_id INTO r_app_list_entry_fee_id;
					EXIT WHEN NOT FOUND;

					-- If doing a dry run don't audit
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'ale_ale_id'::text,
														r_app_list_entry_fee_id.ale_ale_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'fee_fee_id'::text,
														r_app_list_entry_fee_id.fee_fee_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'version'::text,
														r_app_list_entry_fee_id.version::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'changed_by'::text,
														r_app_list_entry_fee_id.changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'changed_date'::text,
														r_app_list_entry_fee_id.changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_id'::text,
														'user_name'::text,
														r_app_list_entry_fee_id.user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;

					END IF;

					-- DELETE the record only if not a dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.app_list_entry_fee_id 
                    	WHERE ale_ale_id = r_app_list_entry_fee_id.ale_ale_id;
					END IF;
				END LOOP;
				CLOSE cur_app_list_entry_fee_id;

				-- Move onto app_list_entry_fee_status
				OPEN cur_app_list_entry_fee_status(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_fee_status INTO r_app_list_entry_fee_status;
					EXIT WHEN NOT FOUND;

					-- only audit if not doing a dry run
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_id'::text,
														r_app_list_entry_fee_status.alefs_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_ale_id'::text,
														r_app_list_entry_fee_status.alefs_ale_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_payment_reference'::text,
														r_app_list_entry_fee_status.alefs_payment_reference::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_fee_status'::text,
														r_app_list_entry_fee_status.alefs_fee_status::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_fee_status_date'::text,
														r_app_list_entry_fee_status.alefs_fee_status_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
		
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_version'::text,
														r_app_list_entry_fee_status.alefs_version::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_changed_by'::text,
														r_app_list_entry_fee_status.alefs_changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_changed_date'::text,
														r_app_list_entry_fee_status.alefs_changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_user_name'::text,
														r_app_list_entry_fee_status.alefs_user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_fee_status'::text,
														'alefs_status_creation_date'::text,
														r_app_list_entry_fee_status.alefs_status_creation_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;

					-- DELETE the record only if not doing dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.app_list_entry_fee_status 
                    	WHERE alefs_ale_id = r_app_list_entry_fee_status.alefs_ale_id;
					END IF;					
				END LOOP;
				CLOSE cur_app_list_entry_fee_status;

				-- Move onto app_list_entry_official
				OPEN cur_app_list_entry_official(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_official INTO r_app_list_entry_official;
					EXIT WHEN NOT FOUND;

					-- Only audit if not doing dry run
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'aleo_id'::text,
        												r_app_list_entry_official.aleo_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'ale_ale_id'::text,
														r_app_list_entry_official.ale_ale_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'title'::text,
														r_app_list_entry_official.title::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'forename'::text,
														r_app_list_entry_official.forename::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'surname'::text,
														r_app_list_entry_official.surname::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'official_type'::text,
														r_app_list_entry_official.official_type::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'changed_by'::text,
														r_app_list_entry_official.changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'changed_date'::text,
														r_app_list_entry_official.changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'app_list_entry_official'::text,
														'user_name'::text,
														r_app_list_entry_official.user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;	
	
					-- DELETE the record only if not doing dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.app_list_entry_official 
                    	WHERE ale_ale_id = r_app_list_entry_official.ale_ale_id;
					END IF;	
				END LOOP;
				CLOSE cur_app_list_entry_official;
		
				-- Move onto al_ale_sequence_mapping
				OPEN cur_al_ale_sequence_mapping(r_application_list.al_id);
				LOOP
					FETCH cur_al_ale_sequence_mapping INTO r_al_ale_sequence_mapping;
					EXIT WHEN NOT FOUND;

					-- only do data audit if not doing dry run
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'al_ale_sequence_mapping'::text,
														'al_id'::text,
														r_al_ale_sequence_mapping.al_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'al_ale_sequence_mapping'::text,
														'ale_last_sequence'::text,
														r_al_ale_sequence_mapping.ale_last_sequence::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;

					-- DELETE the record only if not doing dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.al_ale_sequence_mapping 
                    	WHERE al_id = r_al_ale_sequence_mapping.al_id;
					END IF;
				END LOOP;
				CLOSE cur_al_ale_sequence_mapping;

				-- Move onto application_register
				OPEN cur_application_register(r_application_list.al_id);
				LOOP
					FETCH cur_application_register INTO r_application_register;
					EXIT WHEN NOT FOUND;

					-- only do data audit if not doing dry run
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'ar_id'::text,
														r_application_register.ar_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'al_al_id'::text,
														r_application_register.al_al_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'text'::text,
														r_application_register.text::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'Y'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'changed_by'::text,
														r_application_register.changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'changed_date'::text,
														r_application_register.changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_register'::text,
														'user_name'::text,
														r_application_register.user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;

					-- DELETE the record only if not doing dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.application_register 
                    	WHERE al_al_id = r_application_register.al_al_id;
					END IF;
					
				END LOOP;
				CLOSE cur_application_register;

				-- Finally if we get here, delete the application_list_entries
				OPEN cur_application_list_entries(r_application_list.al_id);
				LOOP
					FETCH cur_application_list_entries INTO r_application_list_entries;
					EXIT WHEN NOT FOUND;

					-- only data audit if not doing dry run
					IF NOT p_return_count THEN
						IF l_data_audit THEN
							-- we are going to audit this record, write the data_audit record
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'ale_id'::text,
														r_application_list_entries.ale_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
		
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'al_al_id'::text,
														r_application_list_entries.al_al_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'sa_sa_id'::text,
														r_application_list_entries.sa_sa_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'ac_ac_id'::text,
														r_application_list_entries.ac_ac_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'a_na_id'::text,
														r_application_list_entries.a_na_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'r_na_id'::text,
														r_application_list_entries.r_na_id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'number_of_bulk_respondents'::text,
														r_application_list_entries.number_of_bulk_respondents::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'application_list_entry_wording'::text,
														r_application_list_entries.application_list_entry_wording::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'Y'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'case_reference'::text,
														r_application_list_entries.case_reference::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'account_number'::text,
														r_application_list_entries.account_number::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'entry_rescheduled'::text,
														r_application_list_entries.entry_rescheduled::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'notes'::text,
														r_application_list_entries.notes::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'version'::text,
														r_application_list_entries.version::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'changed_by'::text,
														r_application_list_entries.changed_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'changed_date'::text,
														r_application_list_entries.changed_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'bulk_upload'::text,
														r_application_list_entries.bulk_upload::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'user_name'::text,
														r_application_list_entries.user_name::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'sequence_number'::text,
														r_application_list_entries.sequence_number::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'tcep_status'::text,
														r_application_list_entries.tcep_status::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);

							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'message_uuid'::text,
														r_application_list_entries.message_uuid::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'retry_count'::text,
														r_application_list_entries.retry_count::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'lodgement_date'::text,
														r_application_list_entries.lodgement_date::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'id'::text,
														r_application_list_entries.id::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'delete_by'::text,
														r_application_list_entries.delete_by::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'delete_date'::text,
														r_application_list_entries.delete_date::text,
														l_database_job::text,
												        'Scheduled Job'::text,
														'N'::text);
	
							CALL ${flyway:defaultSchema}.write_data_audit_table('${flyway:defaultSchema}'::text,
														'application_list_entries'::text,
														'is_deleted'::text,
														r_application_list_entries.is_deleted::text,
														l_database_job::text,
														'Scheduled Job'::text,
														'N'::text);
						END IF;
					END IF;

					-- DELETE the record only if not doing dry run
					IF NOT p_return_count THEN
						DELETE FROM ${flyway:defaultSchema}.application_list_entries 
                    	WHERE al_al_id = r_application_list_entries.al_al_id;
					END IF;
				END LOOP;
				CLOSE cur_application_list_entries;

				-- Increment the success count
				l_success_counter = l_success_counter + 1;
	
				-- update the APPLICATION_LIST
				-- only run if not in a dry run
				IF NOT p_return_count THEN
					UPDATE ${flyway:defaultSchema}.application_lists
						SET child_deleted = TRUE
						WHERE al_id = r_application_list.al_id;
				END IF;	
			END LOOP;
				
			CLOSE cur_application_lists;
		END IF;
		
		l_result_string = concat('Job ',l_database_job,' successfully completed at ',NOW());
		-- Only write to the job status table if not doing a dry run
		IF NOT p_return_count THEN
			CALL ${flyway:defaultSchema}.write_job_status_table(
				l_database_job,
				l_execution_start_time,
				now()::timestamp,
				'SUCCESS',
				l_number_evaluated,
				l_success_counter,
				l_result_string,
				l_parameters_used);
	
			-- update the job table to show the last run date
			UPDATE ${flyway:defaultSchema}.database_jobs
				SET job_last_ran = NOW()
				WHERE job_name = l_database_job;
		END IF;
		
		IF p_return_count THEN
			RETURN l_success_counter;
		END IF;
		RETURN l_success_counter;
	EXCEPTION
		WHEN OTHERS THEN
			l_error_string = concat('Job ',l_database_job,' has errors: ',SQLERRM,' ',SQLSTATE);
			-- Write to the job status table if not doing a dry run
			IF NOT p_return_count THEN
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'FAILED',
					0,
					0,
					l_error_string,
					l_parameters_used);
			END IF;
			
			IF p_return_count THEN
				RETURN 0;
			END IF;

			RETURN NULL;
			
	END;

END;
$BODY$;

-- Create the new function delete_expired_application_lists_count
CREATE OR REPLACE FUNCTION delete_expired_application_lists_count(
	)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
BEGIN
    RETURN ${flyway:defaultSchema}.delete_expired_application_lists_core(true);
END;
$BODY$;

-- Drop the stored procedure delete_expired_application_lists
DROP PROCEDURE IF EXISTS delete_expired_application_lists();

-- Create the new version of stored procedure delete_expired_application_lists
CREATE OR REPLACE PROCEDURE delete_expired_application_lists(
	)
LANGUAGE 'plpgsql'
AS $BODY$
BEGIN
    PERFORM ${flyway:defaultSchema}.delete_expired_application_lists_core(false);
END;
$BODY$;

-- Drop the stored procedure write_job_status_table
DROP PROCEDURE IF EXISTS write_job_status_table(
    job_name text,
    job_start_time timestamp,
    job_end_time timestamp,
    job_status text,
    number_evaluated integer,
    number_success integer,
    result_string text);

-- Create the new version of stored procedure write_job_status_table with an extra parameter for parameters used
CREATE OR REPLACE PROCEDURE write_job_status_table(
	IN p_database_job text,
	IN p_start_time timestamp without time zone,
	IN p_end_time timestamp without time zone,
	IN p_execution_status text,
	IN p_number_evaluated_records bigint,
	IN p_number_deleted_records bigint,
	IN p_execution_message text,
	IN p_parameters_used text)
LANGUAGE 'plpgsql'
AS $BODY$

BEGIN
	INSERT INTO ${flyway:defaultSchema}.database_job_execution_log values(
		nextval('${flyway:defaultSchema}.djel_seq'),
		p_database_job::text,
		p_start_time::timestamp,
		p_end_time::timestamp,
		p_execution_status::text,
		p_number_evaluated_records::bigint,
		p_number_deleted_records::bigint,
		p_execution_message::text,
		p_parameters_used::text);
END;
$BODY$;

-- V41__database_jobs_arcpoc996.sql

-- Version Control
-- V1.0  	Matthew Harman  26/03/2026	Initial Version
--

CREATE TABLE database_jobs (
    dj_id SERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,
    job_enabled CHAR(1) DEFAULT 'N' CHECK (job_enabled IN ('Y','N')),
    job_last_ran TIMESTAMP
);

CREATE TABLE retention_policy (
    rp_id SERIAL PRIMARY KEY,
    dj_dj_id BIGINT,
    retention_policy_name VARCHAR(255) NOT NULL,
    retention_policy_start_date DATE NOT NULL,
    retention_policy_end_date DATE,
    retention_policy_metadata VARCHAR(1000)
);

CREATE TABLE retention_policy_configuration (
    rpc_id SERIAL PRIMARY KEY,
    rp_rp_id bigint NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    config_notes VARCHAR(1000)
);

ALTER TABLE retention_policy ADD CONSTRAINT rp_dj_fk FOREIGN KEY (dj_dj_id)
    REFERENCES database_jobs(dj_id);

DROP SEQUENCE IF EXISTS dj_seq;
CREATE SEQUENCE dj_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

DROP SEQUENCE IF EXISTS rp_seq;
CREATE SEQUENCE rp_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

DROP SEQUENCE IF EXISTS rpc_seq;
CREATE SEQUENCE rpc_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

ALTER TABLE retention_policy_configuration ADD CONSTRAINT rpc_rp_fk FOREIGN KEY (rp_rp_id)
    REFERENCES retention_policy(rp_id);

CREATE TABLE database_job_execution_log (
    djel_id SERIAL PRIMARY KEY,
    job_name VARCHAR(255) NOT NULL,  
    execution_start_time TIMESTAMP NOT NULL,
    execution_end_time TIMESTAMP NOT NULL,
    execution_status VARCHAR(255) NOT NULL,
    number_evaluated_records BIGINT,
    number_deleted_records BIGINT,
    execution_message VARCHAR(1000)
);

DROP SEQUENCE IF EXISTS djel_seq;
CREATE SEQUENCE djel_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

-- Modify APPLICATION_LISTS to have an additional field CHILD_DELETED to support child record deletion tracking for application lists
-- of type BOOLEAN, defaulted to FALSE, and not null
ALTER TABLE application_lists ADD COLUMN child_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Add in the code
-- write_data_audit_table - this sp writes to the data audit table
DROP PROCEDURE IF EXISTS write_data_audit_table;

CREATE OR REPLACE PROCEDURE write_data_audit_table(
	IN p_schema text,
	IN p_table_name text,
	IN p_column_name text,
	IN p_old_value text,
	IN p_event_name text,
	IN p_user_name text,
	IN p_clob text)
LANGUAGE 'plpgsql'
AS $BODY$

BEGIN
	IF p_clob = 'N' THEN
		INSERT INTO ${flyway:defaultSchema}.data_audit values(
			nextval('${flyway:defaultSchema}.add_dataaudit_event'),
			p_schema::text,
			p_table_name::text,
			p_column_name::text,
			p_old_value::text,
			NULL,
			NULL,
			NULL,
			NOW()::timestamp,
			NULL,
			NULL,
			NULL,
			'D'::text,
			NULL,
			NULL,
			NULL,
			p_event_name,
			p_user_name);
	ELSE
		INSERT INTO ${flyway:defaultSchema}.data_audit values(
			nextval('${flyway:defaultSchema}.add_dataaudit_event'),
			p_schema::text,
			p_table_name::text,
			p_column_name::text,
			NULL,
			NULL,
			NULL,
			NULL,
			NOW()::timestamp,
			p_old_value::text,
			NULL,
			NULL,
			'D'::text,
			NULL,
			NULL,
			NULL,
			p_event_name,
			p_user_name);
	END IF;
END;
$BODY$;

-- write_job_status_table - this sp writes to the job_status table (database_job_execution_log)
DROP PROCEDURE IF EXISTS write_job_status_table;

CREATE OR REPLACE PROCEDURE write_job_status_table(
	IN p_database_job text,
	IN p_start_time timestamp without time zone,
	IN p_end_time timestamp without time zone,
	IN p_execution_status text,
	IN p_number_evaluated_records bigint,
	IN p_number_deleted_records bigint,
	IN p_execution_message text)
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
		p_execution_message::text);
END;
$BODY$;

-- is_database_job_enabled - this function checks if a database job is enabled or not and returns a boolean value
DROP FUNCTION IF EXISTS is_database_job_enabled(text);

CREATE OR REPLACE FUNCTION is_database_job_enabled(
	p_job_name text)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
 
DECLARE 
	p_count INTEGER;
	p_job_enabled TEXT;
	p_result BOOLEAN;
BEGIN 
	BEGIN
		SELECT count(*) 
            INTO p_count 
            FROM ${flyway:defaultSchema}.database_jobs 
            WHERE job_name = p_job_name;

		IF p_count = 1 THEN
			-- determine if the job is enabled
			SELECT job_enabled 
			    INTO p_job_enabled
			    FROM ${flyway:defaultSchema}.database_jobs
			    WHERE job_name = p_job_name;

			IF p_job_enabled IN ('Y','y','TRUE','true') THEN
				p_result = TRUE;
			ELSIF p_job_enabled IN ('N','n','FALSE','false') THEN
				p_result = FALSE;
				RAISE EXCEPTION USING
					ERRCODE = 'JE004', 
					MESSAGE = format('job "%s" is not enabled', p_job_name); 
			ELSE
				RAISE EXCEPTION USING
					ERRCODE = 'JE003', 
					MESSAGE = format('job "%s" is not in a determinable state, should be Y|y|N|n|TRUE|true|FALSE|false', p_job_name); 
			END IF;	
		ELSIF p_count > 1 THEN
			RAISE EXCEPTION USING
				ERRCODE = 'JE002', 
				MESSAGE = format('job "%s" is created multiple times', p_job_name); 
		ELSIF p_count = 0 THEN		
			RAISE EXCEPTION USING
				ERRCODE = 'JE001', 
				MESSAGE = format('job "%s" is not created', p_job_name); 
		END IF;
		RETURN p_result;
	END;
END; 
$BODY$;

-- is_database_job_created - this function checks if a database job is created or not and returns a boolean value
DROP FUNCTION IF EXISTS is_database_job_created(text);

CREATE OR REPLACE FUNCTION is_database_job_created(
	p_job_name text)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
 
DECLARE 
v_count BIGINT; 
BEGIN 
	SELECT COUNT(*) 
        INTO v_count 
        FROM ${flyway:defaultSchema}.database_jobs 
        WHERE job_name = p_job_name; 
	
	IF v_count > 1 THEN RAISE EXCEPTION USING 
		ERRCODE = 'JC001', 
		MESSAGE = format('job "%s" is created multiple times', p_job_name); 
	ELSIF v_count = 0 THEN RAISE EXCEPTION USING 
		ERRCODE = 'JC002', 
		MESSAGE = format('job "%s" is not created', p_job_name); 
	END IF; 
	
	RETURN true; 
END; 
$BODY$;

-- get_retention_policy_id - this function returns the retention policy id for a given database job name
DROP FUNCTION IF EXISTS get_retention_policy_id(text);

CREATE OR REPLACE FUNCTION get_retention_policy_id(
	p_job_name text)
    RETURNS bigint
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
 
DECLARE 
	p_rp_id BIGINT;
BEGIN 
	BEGIN
		SELECT rp_id 
            INTO p_rp_id
		    FROM ${flyway:defaultSchema}.retention_policy
		    WHERE dj_dj_id = (SELECT dj_id 
						        FROM ${flyway:defaultSchema}.database_jobs 
						        WHERE job_name = p_job_name)
		    AND retention_policy_start_date <= now()
		    AND COALESCE(retention_policy_end_date,now()) >= now();

	EXCEPTION
		WHEN OTHERS THEN RAISE EXCEPTION USING
			ERRCODE = 'JP001', 
			MESSAGE = format('job "%s" does not have a retention policy', p_job_name); 
	END;

	RETURN p_rp_id;
END; 
$BODY$;

-- get_retention_policy_parameter - this function returns the retention policy parameter value for a given retention policy id and parameter name
DROP FUNCTION IF EXISTS get_retention_policy_parameter(text, bigint);

CREATE OR REPLACE FUNCTION get_retention_policy_parameter(
	p_parameter_name text,
	p_rp_id bigint)
    RETURNS text
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
DECLARE 
	p_policy_value text;
BEGIN 
	BEGIN
		SELECT config_value 
            INTO p_policy_value
		    FROM ${flyway:defaultSchema}.retention_policy_configuration
		    WHERE rp_rp_id = p_rp_id
		    AND config_key = p_parameter_name;
	EXCEPTION
		WHEN OTHERS THEN RAISE EXCEPTION USING
			ERRCODE = 'JO001', 
			MESSAGE = format('job "%s" does not have a value for %s', p_job_name, p_policy_name); 
	END;

	RETURN p_policy_value;
END; 
$BODY$;

-- delete_expired_application_lists - this sp deletes expired application lists based on the retention policy parameters for the APPLICATION_LISTS_DATABASE_JOB
DROP PROCEDURE IF EXISTS delete_expired_application_lists();

CREATE OR REPLACE PROCEDURE delete_expired_application_lists(
	)
LANGUAGE 'plpgsql'
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
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job::text,
					l_execution_start_time::timestamp,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
			WHEN SQLSTATE 'JC002' THEN
				l_error_string = concat('Job ',l_database_job,' does not have a record in the database_jobs table');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;	

		-- check job is enabled
    	BEGIN
			PERFORM ${flyway:defaultSchema}.is_database_job_enabled(l_database_job);
		EXCEPTION
			WHEN SQLSTATE 'JE001' THEN
				l_error_string = concat('Job ',l_database_job,' does not exist');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
			WHEN SQLSTATE 'JE002' THEN
				l_error_string = concat('Job ',l_database_job,' is created multiple times');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
			WHEN SQLSTATE 'JE003' THEN
				l_error_string = concat('Job ',l_database_job,' is in indeterminable state');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
			WHEN SQLSTATE 'JE004' THEN
				l_error_string = concat('Job ',l_database_job,' is not enabled');
	
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;	

		-- check we have retention policy
		BEGIN
			SELECT ${flyway:defaultSchema}.get_retention_policy_id(l_database_job) INTO l_rp_id;
		EXCEPTION
			WHEN SQLSTATE 'JP001' THEN
				l_error_string = concat('Job ',l_database_job,' does not have a retention policy');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;

		-- get the retention period
		l_policy_parameter = 'RETENTION_PERIOD_DAYS';
		BEGIN
			SELECT ${flyway:defaultSchema}.get_retention_policy_parameter(l_policy_parameter,l_rp_id) INTO l_retention_period;
		EXCEPTION
			WHEN SQLSTATE 'JO001' THEN
				l_error_string = concat('Job ',l_database_job,' does not have policy parameter ',l_policy_parameter);
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;

		-- convert the retention period into an integer
		BEGIN
			SELECT CAST(l_retention_period AS INTEGER) INTO l_retention_period_value;
		EXCEPTION
			WHEN OTHERS THEN
				l_error_string = concat('Job ',l_database_job,' retention period is not a number of days');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;
	
		-- get the retention period
		l_policy_parameter = 'ENABLE_DATA_AUDIT';
		BEGIN
			SELECT ${flyway:defaultSchema}.get_retention_policy_parameter(l_policy_parameter,l_rp_id) INTO l_enable_data_audit;
		EXCEPTION
			WHEN SQLSTATE 'JO001' THEN
				l_error_string = concat('Job ',l_database_job,' does not have policy parameter ',l_policy_parameter);
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
		END;

		-- check enable_data_audit is Y|y|N|n|TRUE|FALSE|true|false
		BEGIN
			IF l_enable_data_audit IN ('Y','y','TRUE','true') THEN
				l_data_audit = TRUE;
			ELSIF l_enable_data_audit IN ('N','n','FALSE','false') THEN
				l_data_audit = FALSE;
			ELSE
				l_error_string = concat('Job ',l_database_job,' enable_data_audit is not a value of Y|y|N|n|TRUE|FALSE|true|false');
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'SKIPPED',
					0,
					0,
					l_error_string);
				RETURN;
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

					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'aler_id'::text,
													r_app_list_entry_resolutions.aler_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
    												'N'::text);
	
    					CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'rc_rc_id'::text,
													r_app_list_entry_resolutions.rc_rc_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
						
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'ale_ale_id'::text,
													r_app_list_entry_resolutions.ale_ale_id::text,
													l_database_job::text,
												    'Scheduled Job'::text,
													'N'::text);
															
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'al_entry_resolution_wording'::text,
													r_app_list_entry_resolutions.al_entry_resolution_wording::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'Y'::text);
																
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'al_entry_resolution_officer'::text,
													r_app_list_entry_resolutions.al_entry_resolution_officer::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
																
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'version'::text,
													r_app_list_entry_resolutions.version::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
																
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'changed_by'::text,
													r_app_list_entry_resolutions.changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
																
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'changed_date'::text,
													r_app_list_entry_resolutions.changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
																
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'user_name'::text,
													r_app_list_entry_resolutions.user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
															
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_resolutions'::text,
													'id'::text,
													r_app_list_entry_resolutions.id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.app_list_entry_resolutions 
                    WHERE aler_id = r_app_list_entry_resolutions.aler_id;
				
				END LOOP;
				CLOSE cur_app_list_entry_resolutions;

				-- Move onto app_list_entry_fee_id
				OPEN cur_app_list_entry_fee_id(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_fee_id INTO r_app_list_entry_fee_id;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'ale_ale_id'::text,
													r_app_list_entry_fee_id.ale_ale_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'fee_fee_id'::text,
													r_app_list_entry_fee_id.fee_fee_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'version'::text,
													r_app_list_entry_fee_id.version::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'changed_by'::text,
													r_app_list_entry_fee_id.changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'changed_date'::text,
													r_app_list_entry_fee_id.changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_id'::text,
													'user_name'::text,
													r_app_list_entry_fee_id.user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.app_list_entry_fee_id 
                    WHERE ale_ale_id = r_app_list_entry_fee_id.ale_ale_id;
					
				END LOOP;
				CLOSE cur_app_list_entry_fee_id;

				-- Move onto app_list_entry_fee_status
				OPEN cur_app_list_entry_fee_status(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_fee_status INTO r_app_list_entry_fee_status;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_id'::text,
													r_app_list_entry_fee_status.alefs_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_ale_id'::text,
													r_app_list_entry_fee_status.alefs_ale_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_payment_reference'::text,
													r_app_list_entry_fee_status.alefs_payment_reference::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_fee_status'::text,
													r_app_list_entry_fee_status.alefs_fee_status::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_fee_status_date'::text,
													r_app_list_entry_fee_status.alefs_fee_status_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_version'::text,
													r_app_list_entry_fee_status.alefs_version::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_changed_by'::text,
													r_app_list_entry_fee_status.alefs_changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_changed_date'::text,
													r_app_list_entry_fee_status.alefs_changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_user_name'::text,
													r_app_list_entry_fee_status.alefs_user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_fee_status'::text,
													'alefs_status_creation_date'::text,
													r_app_list_entry_fee_status.alefs_status_creation_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.app_list_entry_fee_status 
                    WHERE alefs_ale_id = r_app_list_entry_fee_status.alefs_ale_id;
					
				END LOOP;
				CLOSE cur_app_list_entry_fee_status;

				-- Move onto app_list_entry_official
				OPEN cur_app_list_entry_official(r_application_list.al_id);
				LOOP
					FETCH cur_app_list_entry_official INTO r_app_list_entry_official;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'aleo_id'::text,
        											r_app_list_entry_official.aleo_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'ale_ale_id'::text,
													r_app_list_entry_official.ale_ale_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'title'::text,
													r_app_list_entry_official.title::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'forename'::text,
													r_app_list_entry_official.forename::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'surname'::text,
													r_app_list_entry_official.surname::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'official_type'::text,
													r_app_list_entry_official.official_type::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'changed_by'::text,
													r_app_list_entry_official.changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'changed_date'::text,
													r_app_list_entry_official.changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'app_list_entry_official'::text,
													'user_name'::text,
													r_app_list_entry_official.user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;
	
					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.app_list_entry_official 
                    WHERE ale_ale_id = r_app_list_entry_official.ale_ale_id;
						
				END LOOP;
				CLOSE cur_app_list_entry_official;
		
				-- Move onto al_ale_sequence_mapping
				OPEN cur_al_ale_sequence_mapping(r_application_list.al_id);
				LOOP
					FETCH cur_al_ale_sequence_mapping INTO r_al_ale_sequence_mapping;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'al_ale_sequence_mapping'::text,
													'al_id'::text,
													r_al_ale_sequence_mapping.al_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'al_ale_sequence_mapping'::text,
													'ale_last_sequence'::text,
													r_al_ale_sequence_mapping.ale_last_sequence::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.al_ale_sequence_mapping 
                    WHERE al_id = r_al_ale_sequence_mapping.al_id;
					
				END LOOP;
				CLOSE cur_al_ale_sequence_mapping;

				-- Move onto application_register
				OPEN cur_application_register(r_application_list.al_id);
				LOOP
					FETCH cur_application_register INTO r_application_register;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'ar_id'::text,
													r_application_register.ar_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'al_al_id'::text,
													r_application_register.al_al_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'text'::text,
													r_application_register.text::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'Y'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'changed_by'::text,
													r_application_register.changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'changed_date'::text,
													r_application_register.changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_register'::text,
													'user_name'::text,
													r_application_register.user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.application_register 
                    WHERE al_al_id = r_application_register.al_al_id;
					
				END LOOP;
				CLOSE cur_application_register;

				-- Finally if we get here, delete the application_list_entries
				OPEN cur_application_list_entries(r_application_list.al_id);
				LOOP
					FETCH cur_application_list_entries INTO r_application_list_entries;
					EXIT WHEN NOT FOUND;
	
					IF l_data_audit THEN
						-- we are going to audit this record, write the data_audit record
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'ale_id'::text,
													r_application_list_entries.ale_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
		
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'al_al_id'::text,
													r_application_list_entries.al_al_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'sa_sa_id'::text,
													r_application_list_entries.sa_sa_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'ac_ac_id'::text,
													r_application_list_entries.ac_ac_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'a_na_id'::text,
													r_application_list_entries.a_na_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'r_na_id'::text,
													r_application_list_entries.r_na_id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'number_of_bulk_respondents'::text,
													r_application_list_entries.number_of_bulk_respondents::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'application_list_entry_wording'::text,
													r_application_list_entries.application_list_entry_wording::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'Y'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'case_reference'::text,
													r_application_list_entries.case_reference::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'account_number'::text,
													r_application_list_entries.account_number::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'entry_rescheduled'::text,
													r_application_list_entries.entry_rescheduled::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'notes'::text,
													r_application_list_entries.notes::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'version'::text,
													r_application_list_entries.version::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'changed_by'::text,
													r_application_list_entries.changed_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'changed_date'::text,
													r_application_list_entries.changed_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'bulk_upload'::text,
													r_application_list_entries.bulk_upload::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'user_name'::text,
													r_application_list_entries.user_name::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'sequence_number'::text,
													r_application_list_entries.sequence_number::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'tcep_status'::text,
													r_application_list_entries.tcep_status::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'message_uuid'::text,
													r_application_list_entries.message_uuid::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'retry_count'::text,
													r_application_list_entries.retry_count::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'lodgement_date'::text,
													r_application_list_entries.lodgement_date::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'id'::text,
													r_application_list_entries.id::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'delete_by'::text,
													r_application_list_entries.delete_by::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'delete_date'::text,
													r_application_list_entries.delete_date::text,
													l_database_job::text,
											        'Scheduled Job'::text,
													'N'::text);
	
						CALL ${flyway:defaultSchema}.write_data_audit_table('appreg'::text,
													'application_list_entries'::text,
													'is_deleted'::text,
													r_application_list_entries.is_deleted::text,
													l_database_job::text,
													'Scheduled Job'::text,
													'N'::text);

					END IF;

					-- DELETE the record
					DELETE FROM ${flyway:defaultSchema}.application_list_entries 
                    WHERE al_al_id = r_application_list_entries.al_al_id;
						
				END LOOP;
				CLOSE cur_application_list_entries;

				-- Increment the success count
				l_success_counter = l_success_counter + 1;
	
				-- update the APPLICATION_LIST
				UPDATE ${flyway:defaultSchema}.application_lists
					SET child_deleted = TRUE
					WHERE al_id = r_application_list.al_id;
			
			END LOOP;
				
			CLOSE cur_application_lists;
		END IF;
		
		l_result_string = concat('Job ',l_database_job,' successfully completed at ',NOW());
		CALL ${flyway:defaultSchema}.write_job_status_table(
			l_database_job,
			l_execution_start_time,
			now()::timestamp,
			'SUCCESS',
			l_number_evaluated,
			l_success_counter,
			l_result_string);
	
		-- update the job table to show the last run date
		UPDATE ${flyway:defaultSchema}.database_jobs
			SET job_last_ran = NOW()
			WHERE job_name = l_database_job;
	EXCEPTION
		WHEN OTHERS THEN
			l_error_string = concat('Job ',l_database_job,' has errors: ',SQLERRM,' ',SQLSTATE);
				CALL ${flyway:defaultSchema}.write_job_status_table(
					l_database_job,
					l_execution_start_time,
					now()::timestamp,
					'FAILED',
					0,
					0,
					l_error_string);
				RETURN;
	END;
		
END;
$BODY$;


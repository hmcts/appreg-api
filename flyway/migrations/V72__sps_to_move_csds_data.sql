-- v72__sps_to_move_csds_data.sql 

-- Version Control
-- V1.0  	Matthew Harman  15/07/2026	Initial Version
--

CREATE OR REPLACE FUNCTION recreate_application_code(
	p_code ${flyway:defaultSchema}.application_codes,
	p_new_id NUMERIC)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    l_new_key INTEGER;
BEGIN
    IF p_new_id IS NULL THEN
	   	-- load into the next sequence value
    	INSERT INTO ${flyway:defaultSchema}.application_codes (
			ac_id,
        	application_code,
        	application_code_title,
        	application_code_wording,
        	application_legislation,
        	fee_due,
	       	application_code_respondent,
    	    ac_destination_email_address_1,
        	ac_destination_email_address_2,
	        application_code_start_date,
    	    application_code_end_date,
        	bulk_respondent_allowed,
	        version,
    	    changed_by,
        	changed_date,
	        user_name,
    	    ac_fee_reference
	    )
    	VALUES (
			nextval('${flyway:defaultSchema}.ac_new_seq'),
        	p_code.application_code,
        	p_code.application_code_title,
        	p_code.application_code_wording,
        	p_code.application_legislation,
        	p_code.fee_due,
        	p_code.application_code_respondent,
        	p_code.ac_destination_email_address_1,
        	p_code.ac_destination_email_address_2,
        	p_code.application_code_start_date,
        	p_code.application_code_end_date,
        	p_code.bulk_respondent_allowed,
        	p_code.version,
        	p_code.changed_by,
        	p_code.changed_date,
        	p_code.user_name,
        	p_code.ac_fee_reference
    	)
    	RETURNING ac_id INTO l_new_key;
	ELSE
		-- load into the supplied value
    	INSERT INTO ${flyway:defaultSchema}.application_codes (
			ac_id,
        	application_code,
        	application_code_title,
        	application_code_wording,
        	application_legislation,
        	fee_due,
	       	application_code_respondent,
    	    ac_destination_email_address_1,
        	ac_destination_email_address_2,
	        application_code_start_date,
    	    application_code_end_date,
        	bulk_respondent_allowed,
	        version,
    	    changed_by,
        	changed_date,
	        user_name,
    	    ac_fee_reference
	    )
    	VALUES (
			p_new_id,
        	p_code.application_code,
        	p_code.application_code_title,
        	p_code.application_code_wording,
        	p_code.application_legislation,
        	p_code.fee_due,
        	p_code.application_code_respondent,
        	p_code.ac_destination_email_address_1,
        	p_code.ac_destination_email_address_2,
        	p_code.application_code_start_date,
        	p_code.application_code_end_date,
        	p_code.bulk_respondent_allowed,
        	p_code.version,
        	p_code.changed_by,
        	p_code.changed_date,
        	p_code.user_name,
        	p_code.ac_fee_reference
    	)
    	RETURNING ac_id INTO l_new_key;
	END IF;
    RETURN l_new_key;
END;
$BODY$;


CREATE OR REPLACE FUNCTION change_application_code_references(
	p_old_ac_id numeric,
	p_new_ac_id integer)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
BEGIN
	UPDATE ${flyway:defaultSchema}.application_list_entries SET ac_ac_id = p_new_ac_id WHERE ac_ac_id = p_old_ac_id;

END;
$BODY$;

CREATE OR REPLACE FUNCTION update_application_code(
	p_code ${flyway:defaultSchema}.application_codes,
	p_existing_id numeric)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$


DECLARE
    l_new_key INTEGER;
BEGIN
	-- load into the next sequence value
    UPDATE ${flyway:defaultSchema}.application_codes
		SET ac_id = p_code.ac_id,
        	application_code = p_code.application_code,
        	application_code_title = p_code.application_code_title,
        	application_code_wording = p_code.application_code_wording,
        	application_legislation = p_code.application_legislation,
        	fee_due = p_code.fee_due,
	    	application_code_respondent = p_code.application_code_respondent,
    		ac_destination_email_address_1 = p_code.ac_destination_email_address_1,
        	ac_destination_email_address_2 = p_code.ac_destination_email_address_2,
	    	application_code_start_date = p_code.application_code_start_date,
    		application_code_end_date = p_code.application_code_end_date,
        	bulk_respondent_allowed = p_code.bulk_respondent_allowed,
	    	version = p_code.version,
    		changed_by = p_code.changed_by,
        	changed_date = p_code.changed_date,
	    	user_name = p_code.user_name,
    		ac_fee_reference = p_code.ac_fee_reference
	WHERE ac_id = p_existing_id;

	l_new_key = p_code.ac_id;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE PROCEDURE write_csds_realignment_table(
	IN p_legacy_id NUMERIC,
	IN p_modern_id NUMERIC,
	IN p_notes text
)
LANGUAGE 'plpgsql'
AS $BODY$

BEGIN
	INSERT INTO ${flyway:defaultSchema}.csds_realignment(cr_id,
										legacy_id,	
										modern_id,
										notes)
		VALUES (
			nextval('${flyway:defaultSchema}.cr_seq'),
			p_legacy_id,
			p_modern_id,
			p_notes);
END;
$BODY$;

CREATE OR REPLACE PROCEDURE realign_application_codes(
	)
LANGUAGE 'plpgsql'
AS $BODY$


DECLARE
	cur_additional_codes CURSOR FOR
		SELECT ac_id, application_code, application_code_title, application_code_wording, application_legislation,	
		 		fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, 
				application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, 
				changed_date, user_name, ac_fee_reference
			FROM ${flyway:defaultSchema}.application_codes 
			WHERE application_code NOT IN
				(SELECT application_code FROM ${flyway:defaultSchema}.application_codes_master)
			ORDER BY ac_id;
	r_additional_codes ${flyway:defaultSchema}.application_codes%ROWTYPE;
			
	cur_mismatched_codes CURSOR FOR
		SELECT ac.ac_id, ac.application_code, ac.application_code_title, ac.application_code_wording, 
				ac.application_legislation,	ac.fee_due, ac.application_code_respondent, ac.ac_destination_email_address_1, 
				ac.ac_destination_email_address_2, ac.application_code_start_date, ac.application_code_end_date, 
				ac.bulk_respondent_allowed, ac.version, ac.changed_by, ac.changed_date, ac.user_name, ac.ac_fee_reference
			FROM ${flyway:defaultSchema}.application_codes ac
			JOIN ${flyway:defaultSchema}.application_codes_master acm
  			ON ac.ac_id = acm.ac_id
			WHERE ROW(
        		ac.ac_id,
        		ac.application_code,
        		ac.application_code_start_date
      		)
			IS DISTINCT FROM
      		ROW(
        		acm.ac_id,
        		acm.application_code,
        		acm.application_code_start_date
      		)
			AND ac.ac_id < 10000
		ORDER BY ac.ac_id;
	r_mismatched_codes ${flyway:defaultSchema}.application_codes%ROWTYPE;

	cur_legacy_codes CURSOR FOR
		SELECT ac_id, application_code, application_code_title, application_code_wording, application_legislation,
		       fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2,
			   application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by,
			   changed_date, user_name, ac_fee_reference
			FROM ${flyway:defaultSchema}.application_codes_master
			ORDER BY ac_id;
	r_legacy_codes ${flyway:defaultSchema}.application_codes;
	
	l_new_key INTEGER;
	l_existing_record BOOLEAN;
	l_modern_ac_id ${flyway:defaultSchema}.application_codes.ac_id%TYPE;
	r_modern_codes ${flyway:defaultSchema}.application_codes%ROWTYPE;
	v_sqlstate TEXT;
	v_message TEXT;

	l_count INTEGER;
BEGIN
	-- codes that exist in Modern that application_code is not known to Legacy
	OPEN cur_additional_codes;
	LOOP
		FETCH cur_additional_codes INTO r_additional_codes;
		EXIT WHEN NOT FOUND;

		-- create a new application_code record
		SELECT ${flyway:defaultSchema}.recreate_application_code(r_additional_codes,NULL) INTO l_new_key;

		-- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_application_code_references(r_additional_codes.ac_id, l_new_key);
		
		-- Delete the old application_code
		DELETE FROM ${flyway:defaultSchema}.application_codes WHERE ac_id = r_additional_codes.ac_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_additional_codes.ac_id,'ac_id: '||r_additional_codes.ac_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_additional_codes;

	-- codes that exist in Modern that are mismatched with Legacy
	OPEN cur_mismatched_codes;
	LOOP
		FETCH cur_mismatched_codes INTO r_mismatched_codes;
		EXIT WHEN NOT FOUND;

		-- create a new application_code record
		SELECT ${flyway:defaultSchema}.recreate_application_code(r_mismatched_codes,NULL) INTO l_new_key;
		
        -- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_application_code_references(r_mismatched_codes.ac_id, l_new_key);
		
		-- Delete the old application_code
		DELETE FROM ${flyway:defaultSchema}.application_codes WHERE ac_id = r_mismatched_codes.ac_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_mismatched_codes.ac_id,'ac_id: '||r_mismatched_codes.ac_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_mismatched_codes;

	-- records in Legacy, move the keys in Modern so that they align
	OPEN cur_legacy_codes;
	LOOP
		FETCH cur_legacy_codes INTO r_legacy_codes;
		EXIT WHEN NOT FOUND;

		l_existing_record := FALSE;

		BEGIN
			-- how many records do we have with this application code?
			SELECT count(*) INTO l_count 
				FROM ${flyway:defaultSchema}.application_codes
				WHERE application_code = r_legacy_codes.application_code;

			IF l_count = 1 THEN 
				-- we will update this record
				SELECT ac_id
					INTO l_modern_ac_id
					FROM ${flyway:defaultSchema}.application_codes
					WHERE application_code = r_legacy_codes.application_code;
				l_existing_record := TRUE;

				IF l_modern_ac_id = r_legacy_codes.ac_id THEN
					-- they both have the same id, so just an update on the record
				    SELECT ${flyway:defaultSchema}.update_application_code(r_legacy_codes,l_modern_ac_id) INTO l_new_key;
			
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.ac_id,l_modern_ac_id,'ac_id: '||l_modern_ac_id||' has been updated');
				ELSE	
			   		-- create the new record
			   		SELECT ${flyway:defaultSchema}.recreate_application_code(r_legacy_codes,r_legacy_codes.ac_id) INTO l_new_key;
        	   		-- change any references from the old record to the new one
		       		PERFORM ${flyway:defaultSchema}.change_application_code_references(l_modern_ac_id, l_new_key);
		
			   		-- Delete the old application_code
		       		DELETE FROM ${flyway:defaultSchema}.application_codes WHERE ac_id = l_modern_ac_id;

					CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_ac_id,'ac_id: '||l_modern_ac_id||' has been moved to '||l_new_key);
				END IF;
			ELSIF l_count = 0 THEN
				-- no modern record, create it
		   		SELECT ${flyway:defaultSchema}.recreate_application_code(r_legacy_codes,r_legacy_codes.ac_id) INTO l_new_key;
				CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.ac_id,NULL,'ac_id: '||r_legacy_codes.ac_id||' has been created');

			ELSIF l_count > 1 THEN
				-- we need to see if we can find the actual id to operate on
				-- try the start date
				SELECT count(*) INTO l_count 
					FROM ${flyway:defaultSchema}.application_codes
					WHERE application_code = r_legacy_codes.application_code
					AND application_code_start_date = r_legacy_codes.application_code_start_date;
				IF l_count = 1 THEN
					-- we can update this record
					SELECT ac_id
						INTO l_modern_ac_id
						FROM ${flyway:defaultSchema}.application_codes
						WHERE application_code = r_legacy_codes.application_code
						AND application_code_start_date = r_legacy_codes.application_code_start_date;
					l_existing_record := TRUE;

					IF l_modern_ac_id = r_legacy_codes.ac_id THEN
						-- they both have the same id, so just an update on the record
					    SELECT ${flyway:defaultSchema}.update_application_code(r_legacy_codes,l_modern_ac_id) INTO l_new_key;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.ac_id,l_modern_ac_id,'ac_id: '||l_modern_ac_id||' has been updated');
					ELSE	
			   			-- create the new record
			   			SELECT ${flyway:defaultSchema}.recreate_application_code(r_legacy_codes,r_legacy_codes.ac_id) INTO l_new_key;

	        	   		-- change any references from the old record to the new one
			       		PERFORM ${flyway:defaultSchema}.change_application_code_references(l_modern_ac_id, l_new_key);
		
				   		-- Delete the old application_code
		    	   		DELETE FROM ${flyway:defaultSchema}.application_codes WHERE ac_id = l_modern_ac_id;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_ac_id,'ac_id: '||l_modern_ac_id||' has been moved to '||l_new_key);
					END IF;
				ELSE
					-- we are unable to find this one to update, flag it to the screen
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.ac_id,NULL,'modern: '||r_legacy_codes.ac_id||' '||r_legacy_codes.application_code||' '||r_legacy_codes.application_code_start_date||' can''t be found in legacy, ignoring');
				END IF;
			END IF;
		END;

	END LOOP;
	CLOSE cur_legacy_codes;
END;
$BODY$;

CREATE OR REPLACE FUNCTION recreate_resolution_code(
	p_code ${flyway:defaultSchema}.resolution_codes,
	p_new_id NUMERIC)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    l_new_key INTEGER;
BEGIN
    IF p_new_id IS NULL THEN
	   	-- load into the next sequence value
    	INSERT INTO ${flyway:defaultSchema}.resolution_codes (
			rc_id,
        	resolution_code,
        	resolution_code_title,
        	resolution_code_wording,
        	resolution_legislation,
    	    rc_destination_email_address_1,
        	rc_destination_email_address_2,
	        resolution_code_start_date,
    	    resolution_code_end_date,
	        version,
    	    changed_by,
        	changed_date,
	        user_name
	    )
    	VALUES (
			nextval('${flyway:defaultSchema}.rc_new_seq'),
        	p_code.resolution_code,
        	p_code.resolution_code_title,
        	p_code.resolution_code_wording,
        	p_code.resolution_legislation,
        	p_code.rc_destination_email_address_1,
        	p_code.rc_destination_email_address_2,
        	p_code.resolution_code_start_date,
        	p_code.resolution_code_end_date,
        	p_code.version,
        	p_code.changed_by,
        	p_code.changed_date,
        	p_code.user_name
    	)
    	RETURNING rc_id INTO l_new_key;
	ELSE
		-- load into the supplied value
    	INSERT INTO ${flyway:defaultSchema}.resolution_codes (
			rc_id,
        	resolution_code,
        	resolution_code_title,
        	resolution_code_wording,
        	resolution_legislation,
    	    rc_destination_email_address_1,
        	rc_destination_email_address_2,
	        resolution_code_start_date,
    	    resolution_code_end_date,
	        version,
    	    changed_by,
        	changed_date,
	        user_name
	    )
    	VALUES (
			p_new_id,
        	p_code.resolution_code,
        	p_code.resolution_code_title,
        	p_code.resolution_code_wording,
        	p_code.resolution_legislation,
        	p_code.rc_destination_email_address_1,
        	p_code.rc_destination_email_address_2,
        	p_code.resolution_code_start_date,
        	p_code.resolution_code_end_date,
        	p_code.version,
        	p_code.changed_by,
        	p_code.changed_date,
        	p_code.user_name
    	)
    	RETURNING rc_id INTO l_new_key;
	END IF;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE FUNCTION change_resolution_code_references(
	p_old_rc_id numeric,
	p_new_rc_id integer)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
BEGIN
	UPDATE ${flyway:defaultSchema}.app_list_entry_resolutions SET rc_rc_id = p_new_rc_id WHERE rc_rc_id = p_old_rc_id;

END;
$BODY$;

CREATE OR REPLACE FUNCTION update_resolution_code(
	p_code ${flyway:defaultSchema}.resolution_codes,
	p_existing_id numeric)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$


DECLARE
    l_new_key INTEGER;
BEGIN
	-- load into the next sequence value
    UPDATE ${flyway:defaultSchema}.resolution_codes
		SET rc_id = p_code.rc_id,
        	resolution_code = p_code.resolution_code,
        	resolution_code_title = p_code.resolution_code_title,
        	resolution_code_wording = p_code.resolution_code_wording,
        	resolution_legislation = p_code.resolution_legislation,
        	rc_destination_email_address_1 = p_code.rc_destination_email_address_1,
        	rc_destination_email_address_2 = p_code.rc_destination_email_address_2,
	    	resolution_code_start_date = p_code.resolution_code_start_date,
    		resolution_code_end_date = p_code.resolution_code_end_date,
	    	version = p_code.version,
    		changed_by = p_code.changed_by,
        	changed_date = p_code.changed_date,
	    	user_name = p_code.user_name
	WHERE rc_id = p_existing_id;

	l_new_key = p_code.rc_id;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE PROCEDURE realign_resolution_codes(
	)
LANGUAGE 'plpgsql'
AS $BODY$


DECLARE
	cur_additional_codes CURSOR FOR
		SELECT rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_legislation,	
		 	   rc_destination_email_address_1, rc_destination_email_address_2, resolution_code_start_date, resolution_code_end_date, 
			   version, changed_by, changed_date, user_name
			FROM ${flyway:defaultSchema}.resolution_codes 
			WHERE resolution_code NOT IN
				(SELECT resolution_code FROM ${flyway:defaultSchema}.resolution_codes_master)
			ORDER BY rc_id;
	r_additional_codes ${flyway:defaultSchema}.resolution_codes%ROWTYPE;
			
	cur_mismatched_codes CURSOR FOR
		SELECT rc.rc_id, rc.resolution_code, rc.resolution_code_title, rc.resolution_code_wording, rc.resolution_legislation,	
			   rc.rc_destination_email_address_1, rc.rc_destination_email_address_2, rc.resolution_code_start_date, rc.resolution_code_end_date, 
			   rc.version, rc.changed_by, rc.changed_date, rc.user_name
			FROM ${flyway:defaultSchema}.resolution_codes rc
			JOIN ${flyway:defaultSchema}.resolution_codes_master rcm
  			ON rc.rc_id = rcm.rc_id
			WHERE ROW(
        		rc.rc_id,
        		rc.resolution_code,
        		rc.resolution_code_start_date
      		)
			IS DISTINCT FROM
      		ROW(
        		rcm.rc_id,
        		rcm.resolution_code,
        		rcm.resolution_code_start_date
      		)
			AND rc.rc_id < 10000
		ORDER BY rc.rc_id;
	r_mismatched_codes ${flyway:defaultSchema}.resolution_codes%ROWTYPE;

	cur_legacy_codes CURSOR FOR
		SELECT rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_legislation,
		       rc_destination_email_address_1, rc_destination_email_address_2, resolution_code_start_date, resolution_code_end_date,
			   version, changed_by, changed_date, user_name
			FROM ${flyway:defaultSchema}.resolution_codes_master
			ORDER BY rc_id;
	r_legacy_codes ${flyway:defaultSchema}.resolution_codes%ROWTYPE;
	
	l_new_key INTEGER;
	l_existing_record BOOLEAN;
	l_modern_rc_id ${flyway:defaultSchema}.resolution_codes.rc_id%TYPE;
	r_modern_codes ${flyway:defaultSchema}.resolution_codes%ROWTYPE;
	v_sqlstate TEXT;
	v_message TEXT;

	l_count INTEGER;
BEGIN
	-- codes that exist in Modern that resolution_code is not known to Legacy
	OPEN cur_additional_codes;
	LOOP
		FETCH cur_additional_codes INTO r_additional_codes;
		EXIT WHEN NOT FOUND;

		-- create a new resolution_code record
		SELECT ${flyway:defaultSchema}.recreate_resolution_code(r_additional_codes,NULL) INTO l_new_key;

		-- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_resolution_code_references(r_additional_codes.rc_id, l_new_key);
		
		-- Delete the old resolution_code
		DELETE FROM ${flyway:defaultSchema}.resolution_codes WHERE rc_id = r_additional_codes.rc_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_additional_codes.rc_id,'rc_id: '||r_additional_codes.rc_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_additional_codes;

	-- codes that exist in Modern that are mismatched with Legacy
	OPEN cur_mismatched_codes;
	LOOP
		FETCH cur_mismatched_codes INTO r_mismatched_codes;
		EXIT WHEN NOT FOUND;

		-- create a new resolution_code record
		SELECT ${flyway:defaultSchema}.recreate_resolution_code(r_mismatched_codes,NULL) INTO l_new_key;
		
        -- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_resolution_code_references(r_mismatched_codes.rc_id, l_new_key);
		
		-- Delete the old resolution_code
		DELETE FROM ${flyway:defaultSchema}.resolution_codes WHERE rc_id = r_mismatched_codes.rc_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_mismatched_codes.rc_id,'rc_id: '||r_mismatched_codes.rc_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_mismatched_codes;

	-- records in Legacy, move the keys in Modern so that they align
	OPEN cur_legacy_codes;
	LOOP
		FETCH cur_legacy_codes INTO r_legacy_codes;
		EXIT WHEN NOT FOUND;

		l_existing_record := FALSE;

		BEGIN
			-- how many records do we have with this application code?
			SELECT count(*) INTO l_count 
				FROM ${flyway:defaultSchema}.resolution_codes
				WHERE resolution_code = r_legacy_codes.resolution_code;

			IF l_count = 1 THEN 
				-- we will update this record
				SELECT rc_id
					INTO l_modern_rc_id
					FROM ${flyway:defaultSchema}.resolution_codes
					WHERE resolution_code = r_legacy_codes.resolution_code;
				l_existing_record := TRUE;

				IF l_modern_rc_id = r_legacy_codes.rc_id THEN
					-- they both have the same id, so just an update on the record
				    SELECT ${flyway:defaultSchema}.update_resolution_code(r_legacy_codes,l_modern_rc_id) INTO l_new_key;
			
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.rc_id,l_modern_rc_id,'rc_id: '||l_modern_rc_id||' has been updated');
				ELSE	
			   		-- create the new record
			   		SELECT ${flyway:defaultSchema}.recreate_resolution_code(r_legacy_codes,r_legacy_codes.rc_id) INTO l_new_key;

        	   		-- change any references from the old record to the new one
		       		PERFORM ${flyway:defaultSchema}.change_resolution_code_references(l_modern_rc_id, l_new_key);
		
			   		-- Delete the old resolution_code
		       		DELETE FROM ${flyway:defaultSchema}.resolution_codes WHERE rc_id = l_modern_rc_id;

					CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_rc_id,'rc_id: '||l_modern_rc_id||' has been moved to '||l_new_key);
				END IF;
			ELSIF l_count = 0 THEN
				-- no modern record, create it
		   		SELECT ${flyway:defaultSchema}.recreate_resolution_code(r_legacy_codes,r_legacy_codes.rc_id) INTO l_new_key;
				CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.rc_id,NULL,'rc_id: '||r_legacy_codes.rc_id||' has been created');

			ELSIF l_count > 1 THEN
				-- we need to see if we can find the actual id to operate on
				-- try the start date
				SELECT count(*) INTO l_count 
					FROM ${flyway:defaultSchema}.resolution_codes
					WHERE resolution_code = r_legacy_codes.resolution_code
					AND resolution_code_start_date = r_legacy_codes.resolution_code_start_date;
				IF l_count = 1 THEN
					-- we can update this record
					SELECT rc_id
						INTO l_modern_rc_id
						FROM ${flyway:defaultSchema}.resolution_codes
						WHERE resolution_code = r_legacy_codes.resolution_code
						AND resolution_code_start_date = r_legacy_codes.resolution_code_start_date;
					l_existing_record := TRUE;

					IF l_modern_rc_id = r_legacy_codes.rc_id THEN
						-- they both have the same id, so just an update on the record
					    SELECT ${flyway:defaultSchema}.update_resolution_code(r_legacy_codes,l_modern_rc_id) INTO l_new_key;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.rc_id,l_modern_rc_id,'rc_id: '||l_modern_rc_id||' has been updated');
					ELSE	
			   			-- create the new record
			   			SELECT ${flyway:defaultSchema}.recreate_resolution_code(r_legacy_codes,r_legacy_codes.rc_id) INTO l_new_key;

	        	   		-- change any references from the old record to the new one
			       		PERFORM ${flyway:defaultSchema}.change_resolution_code_references(l_modern_rc_id, l_new_key);
		
				   		-- Delete the old resolution_code
		    	   		DELETE FROM ${flyway:defaultSchema}.resolution_codes WHERE rc_id = l_modern_rc_id;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_rc_id,'rc_id: '||l_modern_rc_id||' has been moved to '||l_new_key);
					END IF;
				ELSE
					-- we are unable to find this one to update, flag it to the screen
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_codes.rc_id,NULL,'modern: '||r_legacy_codes.rc_id||' '||r_legacy_codes.resolution_code||' '||r_legacy_codes.resolution_code_start_date||' can''t be found in legacy, ignoring');
				END IF;
			END IF;
		END;

	END LOOP;
	CLOSE cur_legacy_codes;
END;
$BODY$;

CREATE OR REPLACE FUNCTION recreate_national_court_houses(
	p_nch ${flyway:defaultSchema}.national_court_houses,
	p_new_id NUMERIC)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    l_new_key INTEGER;
BEGIN
    IF p_new_id IS NULL THEN
	   	-- load into the next sequence value
    	INSERT INTO ${flyway:defaultSchema}.national_court_houses (
			nch_id,
        	courthouse_name,
        	version_number,
        	changed_by,
        	changed_date,
        	court_type,
	       	start_date,
    	    end_date,
        	loc_loc_id,
	        psa_psa_id,
    	    court_location_code,
        	sl_courthouse_name,
	        norg_id
	    )
    	VALUES (
			nextval('${flyway:defaultSchema}.nch_new_seq'),
        	p_nch.courthouse_name,
        	p_nch.version_number,
        	p_nch.changed_by,
        	p_nch.changed_date,
        	p_nch.court_type,
        	p_nch.start_date,
        	p_nch.end_date,
        	p_nch.loc_loc_id,
        	p_nch.psa_psa_id,
        	p_nch.court_location_code,
        	p_nch.sl_courthouse_name,
        	p_nch.norg_id
    	)
    	RETURNING nch_id INTO l_new_key;
	ELSE
		-- load into the supplied value
    	INSERT INTO ${flyway:defaultSchema}.national_court_houses (
			nch_id,
        	courthouse_name,
        	version_number,
        	changed_by,
        	changed_date,
        	court_type,
			start_date,
			end_date,
			loc_loc_id,
			psa_psa_id,
			court_location_code,
			sl_courthouse_name,
			norg_id
	    )
    	VALUES (
			p_new_id,
        	p_nch.courthouse_name,
        	p_nch.version_number,
        	p_nch.changed_by,
        	p_nch.changed_date,
        	p_nch.court_type,
        	p_nch.start_date,
        	p_nch.end_date,
        	p_nch.loc_loc_id,
        	p_nch.psa_psa_id,
        	p_nch.court_location_code,
        	p_nch.sl_courthouse_name,
        	p_nch.norg_id
    	)
    	RETURNING nch_id INTO l_new_key;
	END IF;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE FUNCTION update_national_court_houses(
	p_nch ${flyway:defaultSchema}.national_court_houses,
	p_existing_id numeric)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$


DECLARE
    l_new_key INTEGER;
BEGIN
	-- load into the next sequence value
    UPDATE ${flyway:defaultSchema}.national_court_houses
		SET nch_id = p_nch.nch_id,
        	courthouse_name = p_nch.courthouse_name,
        	version_number = p_nch.version_number,
        	changed_by = p_nch.changed_by,
        	changed_date = p_nch.changed_date,
        	court_type = p_nch.court_type,
			start_date = p_nch.start_date,
			end_date = p_nch.end_date,
			loc_loc_id = p_nch.loc_loc_id,
			psa_psa_id = p_nch.psa_psa_id,
			court_location_code = p_nch.court_location_code,
			sl_courthouse_name = p_nch.sl_courthouse_name,
			norg_id = p_nch.norg_id
	WHERE nch_id = p_existing_id;

	l_new_key = p_nch.nch_id;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE PROCEDURE realign_national_court_houses(
	)
LANGUAGE 'plpgsql'
AS $BODY$


DECLARE
	cur_additional_nch CURSOR FOR
		SELECT nch_id, courthouse_name, version_number, changed_by, changed_date, court_type, start_date, end_date, loc_loc_id, psa_psa_id, 
			   court_location_code, sl_courthouse_name, norg_id
			FROM ${flyway:defaultSchema}.national_court_houses 
			WHERE courthouse_name NOT IN
				(SELECT courthouse_name FROM ${flyway:defaultSchema}.national_court_houses_master)
			ORDER BY nch_id;
	r_additional_nch ${flyway:defaultSchema}.national_court_houses%ROWTYPE;
			
	cur_legacy_nch CURSOR FOR
		SELECT nch_id, courthouse_name, version_number, changed_by, changed_date, court_type, start_date, end_date, loc_loc_id, psa_psa_id,
		       court_location_code, sl_courthouse_name, norg_id
			FROM ${flyway:defaultSchema}.national_court_houses_master
			ORDER BY nch_id;
	r_legacy_nch ${flyway:defaultSchema}.national_court_houses%ROWTYPE;
	
	l_new_key INTEGER;
	l_existing_record BOOLEAN;
	l_modern_nch_id ${flyway:defaultSchema}.national_court_houses.nch_id%TYPE;
	r_modern_nch ${flyway:defaultSchema}.national_court_houses%ROWTYPE;
	v_sqlstate TEXT;
	v_message TEXT;

	l_count INTEGER;
BEGIN
	-- codes that exist in Modern that application_code is not known to Legacy
	OPEN cur_additional_nch;
	LOOP
		FETCH cur_additional_nch INTO r_additional_nch;
		EXIT WHEN NOT FOUND;

		-- create a new national_court_houses record
		SELECT ${flyway:defaultSchema}.recreate_national_court_houses(r_additional_nch,NULL) INTO l_new_key;

		-- Delete the old national_court_houses
		DELETE FROM ${flyway:defaultSchema}.national_court_houses WHERE nch_id = r_additional_nch.nch_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_additional_nch.nch_id,'nch_id: '||r_additional_nch.nch_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_additional_nch;

	-- codes that exist in Legacy, need to be created in Modern
	OPEN cur_legacy_nch;
	LOOP
		FETCH cur_legacy_nch INTO r_legacy_nch;
		EXIT WHEN NOT FOUND;

			-- no modern record, create it
	   		SELECT ${flyway:defaultSchema}.recreate_national_court_houses(r_legacy_nch,r_legacy_nch.nch_id) INTO l_new_key;

			CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_nch.nch_id,NULL,'nch_id: '||r_legacy_nch.nch_id||' has been created');

	END LOOP;
	CLOSE cur_legacy_nch;
END;
$BODY$;

CREATE OR REPLACE FUNCTION recreate_fee(
	p_fee ${flyway:defaultSchema}.fee,
	p_new_id NUMERIC)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    l_new_key INTEGER;
BEGIN
    IF p_new_id IS NULL THEN
	   	-- load into the next sequence value
    	INSERT INTO ${flyway:defaultSchema}.fee (
			fee_id,
        	fee_reference,
        	fee_description,
        	fee_value,
        	fee_start_date,
        	fee_end_date,
	       	fee_version,
    	    fee_changed_by,
        	fee_changed_date,
	        fee_user_name
	    )
    	VALUES (
			nextval('${flyway:defaultSchema}.fee_new_seq'),
        	p_fee.fee_reference,
        	p_fee.fee_description,
        	p_fee.fee_value,
        	p_fee.fee_start_date,
        	p_fee.fee_end_date,
        	p_fee.fee_version,
        	p_fee.fee_changed_by,
        	p_fee.fee_changed_date,
        	p_fee.fee_user_name
    	)
    	RETURNING fee_id INTO l_new_key;
	ELSE
		-- load into the supplied value
    	INSERT INTO ${flyway:defaultSchema}.fee (
			fee_id,
        	fee_reference,
        	fee_description,
        	fee_value,
        	fee_start_date,
        	fee_end_date,
	       	fee_version,
	       	fee_changed_by,
    	    fee_changed_date,
        	fee_user_name
	    )
    	VALUES (
			p_new_id,
        	p_fee.fee_reference,
        	p_fee.fee_description,
        	p_fee.fee_value,
        	p_fee.fee_start_date,
        	p_fee.fee_end_date,
        	p_fee.fee_version,
        	p_fee.fee_changed_by,
        	p_fee.fee_changed_date,
        	p_fee.fee_user_name
    	)
    	RETURNING fee_id INTO l_new_key;
	END IF;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE FUNCTION change_fee_references(
	p_old_fee_id numeric,
	p_new_fee_id integer)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
BEGIN
	UPDATE ${flyway:defaultSchema}.app_list_entry_fee_id SET fee_fee_id = p_new_fee_id WHERE fee_fee_id = p_old_fee_id;

END;
$BODY$;

CREATE OR REPLACE FUNCTION update_fee(
	p_fee ${flyway:defaultSchema}.fee,
	p_existing_id numeric)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$


DECLARE
    l_new_key INTEGER;
BEGIN
	-- load into the next sequence value
    UPDATE ${flyway:defaultSchema}.fee
		SET fee_id = p_fee.fee_id,
        	fee_reference = p_fee.fee_reference,
        	fee_description = p_fee.fee_description,
        	fee_value = p_fee.fee_value,
        	fee_start_date = p_fee.fee_start_date,
        	fee_end_date = p_fee.fee_end_date,
	    	fee_version = p_fee.fee_version,
    		fee_changed_by = p_fee.fee_changed_by,
        	fee_changed_date = p_fee.fee_changed_date,
	    	fee_user_name = p_fee.fee_user_name
	WHERE fee_id = p_existing_id;

	l_new_key = p_fee.fee_id;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE PROCEDURE realign_fees(
	)
LANGUAGE 'plpgsql'
AS $BODY$


DECLARE
	cur_additional_fees CURSOR FOR
		SELECT fee_id, fee_reference, fee_description, fee_value, fee_start_date, fee_end_date,
		       fee_version, fee_changed_by, fee_changed_date, fee_user_name
			FROM ${flyway:defaultSchema}.fee 
			WHERE fee_reference NOT IN
				(SELECT fee_reference FROM ${flyway:defaultSchema}.fee_master)
			ORDER BY fee_id;
	r_additional_fees ${flyway:defaultSchema}.fee%ROWTYPE;
			
	cur_mismatched_fees CURSOR FOR
		SELECT f.fee_id, f.fee_reference, f.fee_description, f.fee_value, f.fee_start_date, f.fee_end_date,
		       f.fee_version, f.fee_changed_by, f.fee_changed_date, f.fee_user_name
			FROM ${flyway:defaultSchema}.fee f
			JOIN ${flyway:defaultSchema}.fee_master fm
  			ON f.fee_id = fm.fee_id
			WHERE ROW(
        		f.fee_id,
        		f.fee_reference,
        		f.fee_start_date
      		)
			IS DISTINCT FROM
      		ROW(
        		fm.fee_id,
        		fm.fee_reference,
        		fm.fee_start_date
      		)
			AND f.fee_id < 10000
		ORDER BY f.fee_id;
	r_mismatched_fees ${flyway:defaultSchema}.fee%ROWTYPE;

	cur_legacy_fees CURSOR FOR
		SELECT fee_id, fee_reference, fee_description, fee_value, fee_start_date, fee_end_date,
		       fee_version, fee_changed_by, fee_changed_date, fee_user_name
			FROM ${flyway:defaultSchema}.fee_master
			ORDER BY fee_id;
	r_legacy_fees ${flyway:defaultSchema}.fee%ROWTYPE;
	
	l_new_key INTEGER;
	l_existing_record BOOLEAN;
	l_modern_fee_id ${flyway:defaultSchema}.fee.fee_id%TYPE;
	r_modern_fees ${flyway:defaultSchema}.fee%ROWTYPE;
	v_sqlstate TEXT;
	v_message TEXT;

	l_count INTEGER;
BEGIN
	-- fees that exist in Modern that fee_reference is not known to Legacy
	OPEN cur_additional_fees;
	LOOP
		FETCH cur_additional_fees INTO r_additional_fees;
		EXIT WHEN NOT FOUND;

		-- create a new fee record
		SELECT ${flyway:defaultSchema}.recreate_fee(r_additional_fees,NULL) INTO l_new_key;

		-- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_fee_references(r_additional_fees.fee_id, l_new_key);
		
		-- Delete the old fee
		DELETE FROM ${flyway:defaultSchema}.fee WHERE fee_id = r_additional_fees.fee_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_additional_fees.fee_id,'fee_id: '||r_additional_fees.fee_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_additional_fees;

	-- fees that exist in Modern that are mismatched with Legacy
	OPEN cur_mismatched_fees;
	LOOP
		FETCH cur_mismatched_fees INTO r_mismatched_fees;
		EXIT WHEN NOT FOUND;

		-- create a new fee record
		SELECT ${flyway:defaultSchema}.recreate_fee(r_mismatched_fees,NULL) INTO l_new_key;
		
        -- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_fee_references(r_mismatched_fees.fee_id, l_new_key);
		
		-- Delete the old fee
		DELETE FROM ${flyway:defaultSchema}.fee WHERE fee_id = r_mismatched_fees.fee_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_mismatched_fees.fee_id,'fee_id: '||r_mismatched_fees.fee_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_mismatched_fees;
	
	-- records in Legacy, move the keys in Modern so that they align
	OPEN cur_legacy_fees;
	LOOP
		FETCH cur_legacy_fees INTO r_legacy_fees;
		EXIT WHEN NOT FOUND;

		l_existing_record := FALSE;

		BEGIN
			-- how many records do we have with this fee_reference?
			SELECT count(*) INTO l_count 
				FROM ${flyway:defaultSchema}.fee
				WHERE fee_reference = r_legacy_fees.fee_reference
				AND fee_start_date = r_legacy_fees.fee_start_date;

			IF l_count = 1 THEN 
				-- we will update this record
				SELECT fee_id
					INTO l_modern_fee_id
					FROM ${flyway:defaultSchema}.fee
					WHERE fee_reference = r_legacy_fees.fee_reference
					AND fee_start_date = r_legacy_fees.fee_start_date;
				l_existing_record := TRUE;

				IF l_modern_fee_id = r_legacy_fees.fee_id THEN
					-- they both have the same id, so just an update on the record
				    SELECT ${flyway:defaultSchema}.update_fee(r_legacy_fees,l_modern_fee_id) INTO l_new_key;
			
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_fees.fee_id,l_modern_fee_id,'fee_id: '||l_modern_fee_id||' has been updated');
				ELSE	
			   		-- create the new record
			   		SELECT ${flyway:defaultSchema}.recreate_fee(r_legacy_fees,r_legacy_fees.fee_id) INTO l_new_key;
        	   		-- change any references from the old record to the new one
		       		PERFORM ${flyway:defaultSchema}.change_fee_references(l_modern_fee_id, l_new_key);
		
			   		-- Delete the old fee
		       		DELETE FROM ${flyway:defaultSchema}.fee WHERE fee_id = l_modern_fee_id;

					CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_fee_id,'fee_id: '||l_modern_fee_id||' has been moved to '||l_new_key);
				END IF;
			ELSIF l_count = 0 THEN
				-- no modern record, create it
		   		SELECT ${flyway:defaultSchema}.recreate_fee(r_legacy_fees,r_legacy_fees.fee_id) INTO l_new_key;
				CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_fees.fee_id,NULL,'fee_id: '||r_legacy_fees.fee_id||' has been created');

			ELSIF l_count > 1 THEN
				-- we need to see if we can find the actual id to operate on
				-- try the start date and end date
				SELECT count(*) INTO l_count 
					FROM ${flyway:defaultSchema}.fee
					WHERE fee_reference = r_legacy_fees.fee_reference
					AND fee_start_date = r_legacy_fees.fee_start_date
					AND fee_end_date = r_legacy_fees.fee_end_date;
				IF l_count = 1 THEN
					-- we can update this record
					SELECT fee_id
						INTO l_modern_fee_id
						FROM ${flyway:defaultSchema}.fee
						WHERE fee_reference = r_legacy_fees.fee_reference
						AND fee_start_date = r_legacy_fees.fee_start_date
						AND fee_end_date = r_legacy_fees.fee_end_date;
					l_existing_record := TRUE;

					IF l_modern_fee_id = r_legacy_fees.fee_id THEN
						-- they both have the same id, so just an update on the record
					    SELECT ${flyway:defaultSchema}.update_fee(r_legacy_fees,l_modern_fee_id) INTO l_new_key;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_fees.fee_id,l_modern_fee_id,'fee_id: '||l_modern_fee_id||' has been updated');
					ELSE	
			   			-- create the new record
			   			SELECT ${flyway:defaultSchema}.recreate_fee(r_legacy_fees,r_legacy_fees.fee_id) INTO l_new_key;

	        	   		-- change any references from the old record to the new one
			       		PERFORM ${flyway:defaultSchema}.change_fee_references(l_modern_fee_id, l_new_key);
		
				   		-- Delete the old fee
		    	   		DELETE FROM ${flyway:defaultSchema}.fee WHERE fee_id = l_modern_fee_id;

						CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,l_modern_fee_id,'fee_id: '||l_modern_fee_id||' has been moved to '||l_new_key);
					END IF;
				ELSE
					-- we are unable to find this one to update, create a new record
			   		SELECT ${flyway:defaultSchema}.recreate_fee(r_legacy_fees,r_legacy_fees.fee_id) INTO l_new_key;
					CALL ${flyway:defaultSchema}.write_csds_realignment_table(r_legacy_fees.fee_id,NULL,'fee_id: '||r_legacy_fees.fee_id||' has been created');
				END IF;
			END IF;
		END;

	END LOOP;
	CLOSE cur_legacy_fees;
END;
$BODY$;

CREATE OR REPLACE FUNCTION recreate_standard_applicant(
	p_sa ${flyway:defaultSchema}.standard_applicants,
	p_new_id NUMERIC)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    l_new_key INTEGER;
BEGIN
    IF p_new_id IS NULL THEN
	   	-- load into the next sequence value
    	INSERT INTO ${flyway:defaultSchema}.standard_applicants (
			sa_id,
        	standard_applicant_code,
        	standard_applicant_start_date,
        	standard_applicant_end_date,
        	version,
        	changed_by,
	       	changed_date,
    	    user_name,
        	name,
	        title,
    	    forename_1,
        	forename_2,
	        forename_3,
    	    surname,
        	address_l1,
	        address_l2,
			address_l3,
			address_l4,
			address_l5,
			postcode,
			email_address,
			telephone_number,
			mobile_number
	    )
    	VALUES (
			nextval('${flyway:defaultSchema}.sa_new_seq'),
        	p_sa.standard_applicant_code,
        	p_sa.standard_applicant_start_date,
        	p_sa.standard_applicant_end_date,
        	p_sa.version,
        	p_sa.changed_by,
        	p_sa.changed_date,
        	p_sa.user_name,
        	p_sa.name,
        	p_sa.title,
        	p_sa.forename_1,
        	p_sa.forename_2,
        	p_sa.forename_3,
        	p_sa.surname,
        	p_sa.address_l1,
        	p_sa.address_l2,
			p_sa.address_l3,
			p_sa.address_l4,
			p_sa.address_l5,
			p_sa.postcode,
			p_sa.email_address,
			p_sa.telephone_number,
			p_sa.mobile_number
    	)
    	RETURNING sa_id INTO l_new_key;
	ELSE
		-- load into the supplied value
    	INSERT INTO ${flyway:defaultSchema}.standard_applicants (
			sa_id,
        	standard_applicant_code,
        	standard_applicant_start_date,
        	standard_applicant_end_date,
        	version,
        	changed_by,
	       	changed_date,
    	    user_name,
        	name,
	        title,
    	    forename_1,
        	forename_2,
	        forename_3,
    	    surname,
        	address_l1,
	        address_l2,
			address_l3,
			address_l4,
			address_l5,
			postcode,
			email_address,
			telephone_number,
			mobile_number
    	)
    	VALUES (
			p_new_id,
        	p_sa.standard_applicant_code,
        	p_sa.standard_applicant_start_date,
        	p_sa.standard_applicant_end_date,
        	p_sa.version,
        	p_sa.changed_by,
        	p_sa.changed_date,
        	p_sa.user_name,
        	p_sa.name,
        	p_sa.title,
        	p_sa.forename_1,
        	p_sa.forename_2,
        	p_sa.forename_3,
        	p_sa.surname,
        	p_sa.address_l1,
        	p_sa.address_l2,
			p_sa.address_l3,
			p_sa.address_l4,
			p_sa.address_l5,
			p_sa.postcode,
			p_sa.email_address,
			p_sa.telephone_number,
			p_sa.mobile_number
    	)
    	RETURNING sa_id INTO l_new_key;
	END IF;
    RETURN l_new_key;
END;
$BODY$;

CREATE OR REPLACE FUNCTION change_standard_applicant_references(
	p_old_sa_id numeric,
	p_new_sa_id integer)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
BEGIN
	UPDATE ${flyway:defaultSchema}.application_list_entries SET sa_sa_id = p_new_sa_id WHERE sa_sa_id = p_old_sa_id;

END;
$BODY$;

CREATE OR REPLACE PROCEDURE realign_standard_applicants(
	)
LANGUAGE 'plpgsql'
AS $BODY$


DECLARE
	cur_modern_standard_applicants CURSOR FOR
		SELECT sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date, version, changed_by, changed_date,
			   user_name, name, title, forename_1, forename_2, forename_3, surname, address_l1, address_l2, address_l3, address_l4, address_l5,
			   postcode, email_address, telephone_number, mobile_number
			FROM ${flyway:defaultSchema}.standard_applicants
			ORDER BY sa_id;
	r_modern_standard_applicants ${flyway:defaultSchema}.standard_applicants%ROWTYPE;
	
	l_new_key INTEGER;
	l_existing_record BOOLEAN;
	l_modern_sa_id ${flyway:defaultSchema}.application_codes.ac_id%TYPE;
	r_modern_codes ${flyway:defaultSchema}.application_codes%ROWTYPE;
	v_sqlstate TEXT;
	v_message TEXT;

	l_count INTEGER;
BEGIN
	-- codes that exist in Modern will be moved to new keys
	OPEN cur_modern_standard_applicants;
	LOOP
		FETCH cur_modern_standard_applicants INTO r_modern_standard_applicants;
		EXIT WHEN NOT FOUND;

		-- create a new standard_applicant record	
		SELECT ${flyway:defaultSchema}.recreate_standard_applicant(r_modern_standard_applicants,NULL) INTO l_new_key;

		-- change any references from the old record to the new one
		PERFORM ${flyway:defaultSchema}.change_standard_applicant_references(r_modern_standard_applicants.sa_id, l_new_key);
		
		-- Delete the old standard_applicant
		DELETE FROM ${flyway:defaultSchema}.standard_applicants WHERE sa_id = r_modern_standard_applicants.sa_id;
		
		CALL ${flyway:defaultSchema}.write_csds_realignment_table(NULL,r_modern_standard_applicants.sa_id,'sa_id: '||r_modern_standard_applicants.sa_id||' has been moved to: '||l_new_key);
	END LOOP;
	CLOSE cur_modern_standard_applicants;
END;
$BODY$;

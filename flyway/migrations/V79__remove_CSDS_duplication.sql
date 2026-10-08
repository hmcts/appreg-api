-- v79__remove_CSDS_duplication.sql 

-- Version Control
-- V1.0  	Matthew Harman  08/10/2026	Initial Version
--

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication()
LANGUAGE plpgsql
AS $$
DECLARE
    cur_find_duplicate_application_codes CURSOR FOR
        SELECT application_code, COUNT(*) AS count
        FROM ${flyway:defaultSchema}.application_codes
        GROUP BY application_code
        HAVING COUNT(*) > 1;

    r_find_duplicate_application_codes  RECORD;

    cur_find_records_by_application_code CURSOR (p_application_code ${flyway:defaultSchema}.application_codes.application_code%TYPE) FOR
        SELECT ac_id
        FROM ${flyway:defaultSchema}.application_codes
        WHERE application_code = p_application_code;
    
    r_find_records_by_application_code RECORD;  

    r_ac_id                            ${flyway:defaultSchema}.application_codes.ac_id%TYPE;
BEGIN   
    OPEN cur_find_duplicate_application_codes;

    LOOP
        FETCH cur_find_duplicate_application_codes INTO r_find_duplicate_application_codes;
        EXIT WHEN NOT FOUND;

        -- report on the duplicate application codes to the screen
        -- try and find the highest version number on the code, or if not take the highest id
        SELECT ac_id INTO r_ac_id
            FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY application_code ORDER BY version DESC, ac_id DESC) AS rn
                FROM ${flyway:defaultSchema}.application_codes t
                WHERE t.application_code = r_find_duplicate_application_codes.application_code
            ) as ranked
        WHERE rn = 1;

        -- Delete all other duplicates, after moving their existing application_list_entries to the kept record
        OPEN cur_find_records_by_application_code(r_find_duplicate_application_codes.application_code);
        LOOP
            FETCH cur_find_records_by_application_code INTO r_find_records_by_application_code;
            EXIT WHEN NOT FOUND;

            IF r_find_records_by_application_code.ac_id <> r_ac_id THEN
                -- Move application_list_entries to the kept record
                UPDATE ${flyway:defaultSchema}.application_list_entries
                SET ac_ac_id = r_ac_id
                WHERE ac_ac_id = r_find_records_by_application_code.ac_id;

                -- Delete the duplicate record
                DELETE FROM ${flyway:defaultSchema}.application_codes
                    WHERE ac_id = r_find_records_by_application_code.ac_id;

            END IF;
        END LOOP;

        CLOSE cur_find_records_by_application_code;

        RAISE NOTICE 'Duplicate application code: %', r_find_duplicate_application_codes.application_code;
        RAISE NOTICE 'Keeping application code with ID: %', r_ac_id;
    END LOOP;

    CLOSE cur_find_duplicate_application_codes;
END;
$$;
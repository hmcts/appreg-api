-- v79__remove_CSDS_duplication.sql 

-- Version Control
-- V1.0  	Matthew Harman  08/10/2026	Initial Version
--

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication_ac()
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

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication_rc()
LANGUAGE plpgsql
AS $$
DECLARE
    cur_find_duplicate_resolution_codes CURSOR FOR
        SELECT resolution_code, COUNT(*) AS count
        FROM ${flyway:defaultSchema}.resolution_codes
        GROUP BY resolution_code
        HAVING COUNT(*) > 1;

    r_find_duplicate_resolution_codes  RECORD;

    cur_find_records_by_resolution_code CURSOR (p_resolution_code ${flyway:defaultSchema}.resolution_codes.resolution_code%TYPE) FOR
        SELECT rc_id
        FROM ${flyway:defaultSchema}.resolution_codes
        WHERE resolution_code = p_resolution_code;
    
    r_find_records_by_resolution_code RECORD;  

    r_rc_id                            ${flyway:defaultSchema}.resolution_codes.rc_id%TYPE;
BEGIN   
    OPEN cur_find_duplicate_resolution_codes;

    LOOP
        FETCH cur_find_duplicate_resolution_codes INTO r_find_duplicate_resolution_codes;
        EXIT WHEN NOT FOUND;

        -- report on the duplicate resolution codes to the screen
        -- try and find the highest version number on the code, or if not take the highest id
        SELECT rc_id INTO r_rc_id
            FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY resolution_code ORDER BY version DESC, rc_id DESC) AS rn
                FROM ${flyway:defaultSchema}.resolution_codes t
                WHERE t.resolution_code = r_find_duplicate_resolution_codes.resolution_code
            ) as ranked
        WHERE rn = 1;

        -- Delete all other duplicates, after moving their existing application_list_entries to the kept record
        OPEN cur_find_records_by_resolution_code(r_find_duplicate_resolution_codes.resolution_code);
        LOOP
            FETCH cur_find_records_by_resolution_code INTO r_find_records_by_resolution_code;
            EXIT WHEN NOT FOUND;

            IF r_find_records_by_resolution_code.rc_id <> r_rc_id THEN
                -- Move application_list_entries to the kept record
                UPDATE ${flyway:defaultSchema}.app_list_entry_resolutions
                SET rc_rc_id = r_rc_id
                WHERE rc_rc_id = r_find_records_by_resolution_code.rc_id;

                -- Delete the duplicate record
                DELETE FROM ${flyway:defaultSchema}.resolution_codes
                    WHERE rc_id = r_find_records_by_resolution_code.rc_id;

            END IF;
        END LOOP;

        CLOSE cur_find_records_by_resolution_code;

        RAISE NOTICE 'Duplicate resolution code: %', r_find_duplicate_resolution_codes.resolution_code;
        RAISE NOTICE 'Keeping resolution code with ID: %', r_rc_id;
    END LOOP;

    CLOSE cur_find_duplicate_resolution_codes;
END;
$$;

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication_fee()
LANGUAGE plpgsql
AS $$
DECLARE
    cur_find_duplicate_fee CURSOR FOR
        SELECT fee_reference, fee_start_date, COUNT(*) AS count
        FROM ${flyway:defaultSchema}.fee
        GROUP BY fee_reference, fee_start_date
        HAVING COUNT(*) > 1;

    r_find_duplicate_fee  RECORD;

    cur_find_records_by_fee CURSOR (p_fee_reference ${flyway:defaultSchema}.fee.fee_reference%TYPE, p_start_date ${flyway:defaultSchema}.fee.fee_start_date%TYPE) FOR
        SELECT fee_id
        FROM ${flyway:defaultSchema}.fee
        WHERE fee_reference = p_fee_reference AND fee_start_date = p_start_date;
    
    r_find_records_by_fee RECORD;  

    r_fee_id                            ${flyway:defaultSchema}.fee.fee_id%TYPE;
BEGIN   
    OPEN cur_find_duplicate_fee;

    LOOP
        FETCH cur_find_duplicate_fee INTO r_find_duplicate_fee;
        EXIT WHEN NOT FOUND;

        -- report on the duplicate fees to the screen
        -- try and find the highest version number on the fee, or if not take the highest id
        SELECT fee_id INTO r_fee_id
            FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY fee_reference, fee_start_date ORDER BY fee_version DESC, fee_id DESC) AS rn
                FROM ${flyway:defaultSchema}.fee t
                WHERE t.fee_reference = r_find_duplicate_fee.fee_reference AND t.fee_start_date = r_find_duplicate_fee.fee_start_date
            ) as ranked
        WHERE rn = 1;

        -- Delete all other duplicates, after moving their existing application_list_entries to the kept record
        OPEN cur_find_records_by_fee(r_find_duplicate_fee.fee_reference, r_find_duplicate_fee.fee_start_date);
        LOOP
            FETCH cur_find_records_by_fee INTO r_find_records_by_fee;
            EXIT WHEN NOT FOUND;

            IF r_find_records_by_fee.fee_id <> r_fee_id THEN
                -- Move application_list_entries to the kept record
                UPDATE ${flyway:defaultSchema}.app_list_entry_fee_id
                SET fee_fee_id = r_fee_id
                WHERE fee_fee_id = r_find_records_by_fee.fee_id;

                -- Delete the duplicate record
                DELETE FROM ${flyway:defaultSchema}.fee
                    WHERE fee_id = r_find_records_by_fee.fee_id;

            END IF;
        END LOOP;

        CLOSE cur_find_records_by_fee;

        RAISE NOTICE 'Duplicate fee: %', r_find_duplicate_fee.fee_reference;
        RAISE NOTICE 'Keeping fee with ID: %', r_fee_id;
    END LOOP;

    CLOSE cur_find_duplicate_fee;
END;
$$;

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication_nch()
LANGUAGE plpgsql
AS $$
DECLARE
    cur_find_duplicate_nch CURSOR FOR
        SELECT courthouse_name, COUNT(*) AS count
        FROM ${flyway:defaultSchema}.national_court_houses
        GROUP BY courthouse_name
        HAVING COUNT(*) > 1;

    r_find_duplicate_nch  RECORD;

    cur_find_records_by_nch CURSOR (p_courthouse_name ${flyway:defaultSchema}.national_court_houses.courthouse_name%TYPE) FOR
        SELECT nch_id
        FROM ${flyway:defaultSchema}.national_court_houses
        WHERE courthouse_name = p_courthouse_name;
    
    r_find_records_by_nch RECORD;  

    r_nch_id                            ${flyway:defaultSchema}.national_court_houses.nch_id%TYPE;
BEGIN   
    OPEN cur_find_duplicate_nch;

    LOOP
        FETCH cur_find_duplicate_nch INTO r_find_duplicate_nch;
        EXIT WHEN NOT FOUND;

        -- report on the duplicate national court houses to the screen
        -- try and find the highest version number on the code, or if not take the highest id
        SELECT nch_id INTO r_nch_id
            FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY courthouse_name ORDER BY version_number DESC, nch_id DESC) AS rn
                FROM ${flyway:defaultSchema}.national_court_houses t
                WHERE t.courthouse_name = r_find_duplicate_nch.courthouse_name
            ) as ranked
        WHERE rn = 1;

        -- Delete all other duplicates, after moving their existing application_list_entries to the kept record
        OPEN cur_find_records_by_nch(r_find_duplicate_nch.courthouse_name);
        LOOP
            FETCH cur_find_records_by_nch INTO r_find_records_by_nch;
            EXIT WHEN NOT FOUND;

            IF r_find_records_by_nch.nch_id <> r_nch_id THEN
                -- Delete the duplicate record
                DELETE FROM ${flyway:defaultSchema}.national_court_houses
                    WHERE nch_id = r_find_records_by_nch.nch_id;

            END IF;
        END LOOP;

        CLOSE cur_find_records_by_nch;

        RAISE NOTICE 'Duplicate national court house: %', r_find_duplicate_nch.courthouse_name;
        RAISE NOTICE 'Keeping national court house with ID: %', r_nch_id;
    END LOOP;

    CLOSE cur_find_duplicate_nch;
END;
$$;

CREATE OR REPLACE PROCEDURE remove_CSDS_duplication_sa()
LANGUAGE plpgsql
AS $$
DECLARE
    cur_find_duplicate_standard_applicants CURSOR FOR
        SELECT standard_applicant_code, COUNT(*) AS count
        FROM ${flyway:defaultSchema}.standard_applicants
        GROUP BY standard_applicant_code
        HAVING COUNT(*) > 1;

    r_find_duplicate_standard_applicant_codes  RECORD;

    cur_find_records_by_standard_applicant CURSOR (p_standard_applicant_code ${flyway:defaultSchema}.standard_applicants.standard_applicant_code%TYPE) FOR
        SELECT sa_id
        FROM ${flyway:defaultSchema}.standard_applicants
        WHERE standard_applicant_code = p_standard_applicant_code;
    
    r_find_records_by_standard_applicant RECORD;  

    r_sa_id                            ${flyway:defaultSchema}.standard_applicants.sa_id%TYPE;
BEGIN   
    OPEN cur_find_duplicate_standard_applicants;

    LOOP
        FETCH cur_find_duplicate_standard_applicants INTO r_find_duplicate_standard_applicant_codes;
        EXIT WHEN NOT FOUND;

        -- report on the duplicate standard applicants to the screen
        -- try and find the highest version number on the code, or if not take the highest id
        SELECT sa_id INTO r_sa_id
            FROM (
                SELECT t.*, ROW_NUMBER() OVER (PARTITION BY standard_applicant_code ORDER BY version DESC, sa_id DESC) AS rn
                FROM ${flyway:defaultSchema}.standard_applicants t
                WHERE t.standard_applicant_code = r_find_duplicate_standard_applicant_codes.standard_applicant_code
            ) as ranked
        WHERE rn = 1;

        -- Delete all other duplicates, after moving their existing application_list_entries to the kept record
        OPEN cur_find_records_by_standard_applicant(r_find_duplicate_standard_applicant_codes.standard_applicant_code);
        LOOP
            FETCH cur_find_records_by_standard_applicant INTO r_find_records_by_standard_applicant;
            EXIT WHEN NOT FOUND;

            IF r_find_records_by_standard_applicant.sa_id <> r_sa_id THEN
                -- Move application_list_entries to the kept record
                UPDATE ${flyway:defaultSchema}.application_list_entries
                SET sa_sa_id = r_sa_id
                WHERE sa_sa_id = r_find_records_by_standard_applicant.sa_id;

                -- Delete the duplicate record
                DELETE FROM ${flyway:defaultSchema}.standard_applicants
                    WHERE sa_id = r_find_records_by_standard_applicant.sa_id;

            END IF;
        END LOOP;

        CLOSE cur_find_records_by_standard_applicant;

        RAISE NOTICE 'Duplicate standard applicant code: %', r_find_duplicate_standard_applicant_codes.standard_applicant_code;
        RAISE NOTICE 'Keeping standard applicant with ID: %', r_sa_id;
    END LOOP;

    CLOSE cur_find_duplicate_standard_applicants;
END;
$$;

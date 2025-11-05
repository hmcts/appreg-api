-- History
-- Matthew Harman       V1.0    03/11/2025      Initial Version
--
-- Standard_Applicants Company Data Population Script
-- This script populates the Standard_Applicants.Company table with initial data.   
-- It is intended to be run as part of the Flyway migration process.

INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date,
                                 version,  changed_by, changed_date, user_name, name, title, forename_1, forename_2, forename_3, surname, address_l1,
                                 address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number) VALUES
        (31, 'APP031', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000031', 'Global Tech Solutions Ltd', NULL, NULL, NULL, NULL, NULL,'123 Business Park', 'Unit A1', NULL, 'London', NULL,'EC1A 1BB', 'contact@example.com', '01234567890', NULL),
        (32, 'APP032', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000032', 'Sunrise Manufacturing Co', NULL, NULL, NULL, NULL, NULL,'456 Industrial Estate', NULL, NULL, 'Birmingham', NULL,'B1 2CD', 'info@example.com', '01234567891', NULL),
        (33, 'APP033', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000033', 'Premier Consulting Group', NULL, NULL, NULL, NULL, NULL,'789 High Street', 'Floor 3', NULL, 'Manchester', NULL,'M1 3EF', 'hello@example.com', '01234567892', NULL),
        (34, 'APP034', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000034', 'Creative Design Studio', NULL, NULL, NULL, NULL, NULL,'321 Arts Quarter', NULL, NULL, 'Bristol', NULL,'BS1 4GH', 'studio@example.com', '01234567893', NULL),
        (35, 'APP035', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000035', 'Advanced Systems Ltd', NULL, NULL, NULL, NULL, NULL,'654 Technology Park', 'Building B', NULL, 'Cambridge', NULL,'CB2 5IJ', 'support@example.com', '01234567894', NULL),
        (36, 'APP036', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000036', 'Innovative Solutions Inc', NULL, NULL, NULL, NULL, NULL,'987 Research Way', NULL, NULL, 'Oxford', NULL,'OX1 6KL', 'contact@example.com', '01234567895', NULL),
        (37, 'APP037', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000037', 'Dynamic Marketing Agency', NULL, NULL, NULL, NULL, NULL,'159 Commercial Road', 'Suite 200', NULL, 'Leeds', NULL,'LS1 7MN', 'agency@example.com', '01234567896', NULL),
        (38, 'APP038', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000038', 'Professional Services Corp', NULL, NULL, NULL, NULL, NULL,'753 Finance District', NULL, NULL, 'Edinburgh', NULL,'EH1 8OP', 'services@example.com', '01234567897', NULL),
        (39, 'APP039', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000039', 'Excellence Engineering Ltd', NULL, NULL, NULL, NULL, NULL,'852 Innovation Hub', 'Block C', NULL, 'Glasgow', NULL,'G1 9QR', 'info@example.com', '01234567898', NULL),
        (40, 'APP040', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000040', 'Strategic Development Group', NULL, NULL, NULL, NULL, NULL,'951 Corporate Centre', NULL, NULL, 'Cardiff', NULL,'CF10 0ST', 'development@example.com', '01234567899', NULL);


-- We need to create Application_List_Entries for each of these new Standard Applicants
        -- Generate all permutations of application_list_entries for every combination of application_lists, standard_applicants, and application_codes
        -- Only for the Standard Applicants created above (sa_id 31-40)
        INSERT INTO application_list_entries (
            ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents,
            application_list_entry_wording, case_reference, entry_rescheduled, notes, version,
            changed_by, changed_date, bulk_upload, sequence_number, lodgement_date
        )
        SELECT
            ROW_NUMBER() OVER (ORDER BY al.al_id, sa.sa_id, ac.ac_id) + (
                SELECT COALESCE(MAX(ale_id), 0) FROM application_list_entries
            ) AS ale_id,
            al.al_id AS al_al_id,
            sa.sa_id AS sa_sa_id,
            ac.ac_id AS ac_ac_id,
            NULL AS a_na_id,
            NULL AS r_na_id,
            0 AS number_of_bulk_respondents,
            ac.application_code_title AS application_list_entry_wording,
            CONCAT('CASE', LPAD(al.al_id::text, 3, '0'), '-', LPAD(sa.sa_id::text, 3, '0'), '-', LPAD(ac.ac_id::text, 3, '0')) AS case_reference,
            'N' AS entry_rescheduled,
            NULL AS notes,
            1 AS version,
            gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47' AS changed_by,
            CURRENT_TIMESTAMP AS changed_date,
            'N' AS bulk_upload,
            1 AS sequence_number,
            CURRENT_TIMESTAMP AS lodgement_date
        FROM
            application_lists al
        CROSS JOIN
            standard_applicants sa
        CROSS JOIN
            application_codes ac
        WHERE sa.sa_id BETWEEN 31 AND 40
          AND NOT EXISTS (
            SELECT 1 FROM application_list_entries ale
            WHERE ale.al_al_id = al.al_id
              AND ale.sa_sa_id = sa.sa_id
              AND ale.ac_ac_id = ac.ac_id
        );

-- Insert our test data for V10
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('10', 'test_support', 'check_schema_objects_v10_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v10_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check records exist in standard_applicants for the newly added companies
    IF NOT EXISTS (SELECT 1 FROM standard_applicants WHERE sa_id BETWEEN 31 AND 40) THEN
        RAISE EXCEPTION 'Standard Applicants Company data not populated correctly';
    END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
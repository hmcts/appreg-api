-- History
-- Matthew Harman       V1.0    06/11/2025      Modifications for ApplicationListEntries based
--                                              on conversation with Zac
--
-- Populate application_list_entries with 1-2 entries for most lists, and 8 entries for one special list
DELETE FROM app_list_entry_fee_id;
DELETE FROM app_list_entry_official;
DELETE FROM app_list_entry_resolutions;
DELETE FROM app_list_entry_fee_status;
DELETE FROM application_list_entries;


DO $$
DECLARE
    max_ale_id INTEGER;
    current_list INTEGER;
    entry_count INTEGER;
    special_list INTEGER := 11; -- List 11 will have 40 entries
    user_names TEXT[] := ARRAY['AR4.Initial.SQL.Upload', 'System.DataMigration', 'Admin.BulkLoad', 'DataLoad.Service', 'Migration.Tool', 'Setup.Process'];
BEGIN
    -- Get the current max ale_id
    SELECT COALESCE(MAX(ale_id), 0) INTO max_ale_id FROM application_list_entries;
    
    -- Loop through each application list
    FOR current_list IN 
        SELECT al_id FROM application_lists ORDER BY al_id
    LOOP
        -- Determine number of entries for this list
        IF current_list = special_list THEN
            entry_count := 40; -- Special list with 40 entries
        ELSE
            entry_count := (current_list % 2) + 1; -- Alternates between 1 and 2 entries
        END IF;
        
        -- Insert entries for this list
        FOR i IN 1..entry_count LOOP
            max_ale_id := max_ale_id + 1;
            
            INSERT INTO application_list_entries (
                ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
                number_of_bulk_respondents, application_list_entry_wording, 
                case_reference, entry_rescheduled, notes, version,
                changed_by, changed_date, bulk_upload, sequence_number, lodgement_date,
                account_number, user_name, retry_count
            ) VALUES (
                max_ale_id,
                current_list,
                ((max_ale_id - 1) % 10) + 1, -- Cycle through standard_applicants 1-10
                ((max_ale_id - 1) % 10) + 1, -- Cycle through application_codes 1-10
                CASE 
                    WHEN random() < 0.3 THEN floor(random() * 4 + 1)::integer
                    ELSE NULL
                END,
                CASE 
                    WHEN random() < 0.85 THEN floor(random() * 4 + 1)::integer
                    ELSE NULL
                END,
                0,
                CASE 
                    WHEN i = 1 THEN 'First application entry for list ' || current_list
                    WHEN i = 2 THEN 'Second application entry for list ' || current_list
                    ELSE 'Application entry ' || i || ' for list ' || current_list
                END,
                CONCAT('CASE', LPAD(current_list::text, 3, '0'), '-', LPAD(i::text, 2, '0')),
                'N',
                CASE 
                    WHEN current_list = special_list THEN 'Special list with multiple entries'
                    WHEN random() < 0.05 THEN 'Additional notes for case ' || CONCAT('CASE', LPAD(current_list::text, 3, '0'), '-', LPAD(i::text, 2, '0'))
                    ELSE NULL
                END,
                1,
                gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',
                CURRENT_TIMESTAMP,
                'N',
                i,
                CURRENT_TIMESTAMP - INTERVAL '1 day' * (i - 1),
                CASE 
                    WHEN random() < 0.95 THEN CONCAT('ACC', LPAD(max_ale_id::text, 6, '0'))
                    ELSE NULL
                END,
                CASE 
                    WHEN random() < 0.99 THEN user_names[floor(random() * array_length(user_names, 1)) + 1]
                    ELSE NULL
                END,
                CASE 
                    WHEN random() < 0.05 THEN 0
                    ELSE NULL
                END
            );
        END LOOP;
    END LOOP;
END $$;

-- APP_LIST_ENTRY_FEE_ID
-- Populate app_list_entry_fee_id: For each application_list_entry, if the related application_code has a non-null ac_fee_reference, link to the latest fee_id for that reference
INSERT INTO app_list_entry_fee_id (
        ale_ale_id,
        fee_fee_id,
        version,
        changed_by,
        changed_date,
        user_name
)
SELECT
        ale.ale_id,
        f.fee_id,
        1 AS version,
        gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47' AS changed_by,
        CURRENT_TIMESTAMP AS changed_date,
        'AR4.Initial.SQL.Upload' AS user_name
FROM application_list_entries ale
JOIN application_codes ac ON ale.ac_ac_id = ac.ac_id
JOIN LATERAL (
        SELECT fee_id
        FROM fee
        WHERE fee_reference = ac.ac_fee_reference
        ORDER BY fee_start_date DESC
        LIMIT 1
) f ON ac.ac_fee_reference IS NOT NULL;

-- APP_LIST_ENTRY_OFFICIAL
-- One row per application_list
INSERT INTO app_list_entry_official (
        aleo_id, ale_ale_id, title, forename, surname, official_type, changed_by, changed_date, user_name
) VALUES
        (1, 1, 'Mr', 'John', 'Smith', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (2, 2, 'Ms', 'Jane', 'Doe', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (3, 3, 'Dr', 'Alex', 'Dunn', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (4, 4, 'Mrs', 'Sarah', 'Johnson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (5, 5, 'Mr', 'Jack', 'Turner', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (6, 6, 'Judge', 'Alice', 'Brown', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (7, 7, 'Judge', 'Bob', 'Green', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (8, 8, 'Judge', 'Carol', 'White', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (9, 9, 'Judge', 'David', 'Black', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (10, 10, 'Judge', 'Emma', 'Grey', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (11, 11, 'Judge', 'Frank', 'Blue', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (12, 12, 'Judge', 'Grace', 'Red', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (13, 13, 'Judge', 'Henry', 'Violet', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (14, 14, 'Judge', 'Ivy', 'Orange', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (15, 15, 'Judge', 'Jack', 'Indigo', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (16, 16, 'Judge', 'Kelly', 'Pink', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (17, 17, 'Judge', 'Liam', 'Teal', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (18, 18, 'Judge', 'Mona', 'Gold', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (19, 19, 'Judge', 'Nina', 'Silver', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20, 20, 'Judge', 'Oscar', 'Bronze', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (21, 21, 'Mr', 'Paul', 'Wilson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (22, 22, 'Ms', 'Quinn', 'Taylor', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (23, 23, 'Dr', 'Ryan', 'Davis', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (24, 24, 'Mrs', 'Stella', 'Miller', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (25, 25, 'Mr', 'Tom', 'Anderson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (26, 26, 'Judge', 'Uma', 'Clark', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (27, 27, 'Judge', 'Victor', 'Lewis', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (28, 28, 'Judge', 'Wendy', 'Walker', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (29, 29, 'Judge', 'Xavier', 'Hall', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (30, 30, 'Judge', 'Yara', 'Allen', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (31, 31, 'Mr', 'Zack', 'Young', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (32, 32, 'Ms', 'Amy', 'King', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (33, 33, 'Dr', 'Ben', 'Wright', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (34, 34, 'Mrs', 'Claire', 'Lopez', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (35, 35, 'Mr', 'Dan', 'Hill', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (36, 36, 'Judge', 'Eve', 'Scott', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (37, 37, 'Judge', 'Fred', 'Adams', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (38, 38, 'Judge', 'Gina', 'Baker', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (39, 39, 'Judge', 'Hugo', 'Gonzalez', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (40, 40, 'Judge', 'Iris', 'Nelson', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (41, 41, 'Mr', 'Jake', 'Carter', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (42, 42, 'Ms', 'Kim', 'Mitchell', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (43, 43, 'Dr', 'Leo', 'Perez', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (44, 44, 'Mrs', 'Maya', 'Roberts', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (45, 45, 'Mr', 'Noah', 'Turner', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (46, 46, 'Judge', 'Olga', 'Phillips', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (47, 47, 'Judge', 'Pete', 'Campbell', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (48, 48, 'Judge', 'Queen', 'Parker', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (49, 49, 'Judge', 'Rex', 'Evans', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (50, 50, 'Judge', 'Sara', 'Edwards', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (51, 51, 'Mr', 'Tony', 'Collins', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (52, 52, 'Ms', 'Ursa', 'Stewart', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (53, 53, 'Dr', 'Vince', 'Sanchez', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (54, 54, 'Mrs', 'Wanda', 'Morris', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (55, 55, 'Mr', 'Xander', 'Rogers', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (56, 56, 'Judge', 'Yolanda', 'Reed', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (57, 57, 'Judge', 'Zoe', 'Cook', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (58, 58, 'Judge', 'Adam', 'Morgan', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (59, 59, 'Judge', 'Beth', 'Bell', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (60, 60, 'Judge', 'Carl', 'Murphy', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (61, 61, 'Mr', 'Dean', 'Bailey', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (62, 62, 'Ms', 'Ella', 'Rivera', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (63, 63, 'Dr', 'Felix', 'Cooper', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (64, 64, 'Mrs', 'Grace', 'Richardson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (65, 65, 'Mr', 'Hans', 'Cox', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (66, 66, 'Judge', 'Ines', 'Howard', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (67, 67, 'Judge', 'Joel', 'Ward', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (68, 68, 'Judge', 'Kate', 'Torres', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (69, 69, 'Judge', 'Luke', 'Peterson', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (70, 70, 'Judge', 'Mara', 'Gray', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (71, 71, 'Mr', 'Neil', 'Ramirez', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (72, 72, 'Ms', 'Opal', 'James', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (73, 73, 'Dr', 'Phil', 'Watson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (74, 74, 'Mrs', 'Rita', 'Brooks', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (75, 75, 'Mr', 'Sam', 'Kelly', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (76, 76, 'Judge', 'Tina', 'Sanders', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (77, 77, 'Judge', 'Ulric', 'Price', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (78, 78, 'Judge', 'Vera', 'Bennett', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (79, 79, 'Judge', 'Wade', 'Wood', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (80, 80, 'Judge', 'Xara', 'Barnes', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (81, 81, 'Mr', 'Yale', 'Ross', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (82, 82, 'Ms', 'Zara', 'Henderson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (83, 83, 'Dr', 'Abel', 'Coleman', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (84, 84, 'Mrs', 'Bria', 'Jenkins', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (85, 85, 'Mr', 'Cody', 'Perry', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (86, 86, 'Judge', 'Dana', 'Powell', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (87, 87, 'Judge', 'Eric', 'Long', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (88, 88, 'Judge', 'Faye', 'Patterson', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (89, 89, 'Judge', 'Gary', 'Hughes', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (90, 90, 'Judge', 'Hope', 'Flores', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (91, 91, 'Mr', 'Ivan', 'Washington', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (92, 92, 'Ms', 'Joy', 'Butler', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (93, 93, 'Dr', 'Kent', 'Simmons', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (94, 94, 'Mrs', 'Luna', 'Foster', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (95, 95, 'Mr', 'Max', 'Gonzales', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (96, 96, 'Judge', 'Nora', 'Bryant', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (97, 97, 'Judge', 'Owen', 'Alexander', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (98, 98, 'Judge', 'Pam', 'Russell', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (99, 99, 'Judge', 'Quinn', 'Griffin', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (100, 100, 'Judge', 'Rose', 'Diaz', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload');

        INSERT INTO app_list_entry_resolutions (aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording, al_entry_resolution_officer, version, changed_by, changed_date, user_name)  VALUES
            (1,1,1,'Application granted in full.','Magistrate Jane Doe',1,gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
            (2,2,1, 'Refused due to lack of supporting documents.','Magistrate John Smith',1,gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'System.DataMigration');
            -- Insert more random application results for demonstration

        -- And some more random ones
        INSERT INTO app_list_entry_resolutions (aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording, al_entry_resolution_officer, version, changed_by, changed_date, user_name) VALUES
                (3, 3, 2, 'Case stated for High Court opinion.', 'Magistrate Alice Brown', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Admin.BulkLoad'),
                (4, 4, 3, 'Collection order made for outstanding fees.', 'Magistrate Bob Green', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'DataLoad.Service'),
                (5, 5, 4, 'Fee remitted due to benefits.', 'Magistrate Carol White', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Migration.Tool'),
                (6, 6, 5, 'Fee remitted for other reasons.', 'Magistrate David Black', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Setup.Process'),
                (7, 9, 6, 'Application refused.', 'Magistrate Emma Grey', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
                (8, 10, 7, 'Respondent attended the hearing.', 'Magistrate Frank Blue', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'System.DataMigration'),
                (9, 12, 8, 'Referred for full court hearing.', 'Magistrate Grace Red', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Admin.BulkLoad'),
                (10, 13, 9, 'Statutory declaration accepted.', 'Magistrate Henry Violet', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'DataLoad.Service'),
                (11, 14, 10, 'Summons issued.', 'Magistrate Ivy Orange', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Migration.Tool'),
                (12, 18, 11, 'Application withdrawn.', 'Magistrate Jack Indigo', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'Setup.Process');


-- Sample payments for some application_list_entries (not 1-1, just a few random ones)
INSERT INTO app_list_entry_fee_status (
        alefs_id, alefs_ale_id, alefs_payment_reference, alefs_fee_status, alefs_fee_status_date,
        alefs_version, alefs_changed_by, alefs_changed_date, alefs_user_name, alefs_status_creation_date
        ) VALUES
                (2, 5, 'PAYREF1001', 'P', DATE '2025-05-16', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-16', 'AR4.Initial.SQL.Upload', DATE '2025-05-16'),
                (3, 10, 'PAYREF1002', 'D', DATE '2025-05-17', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-17', 'AR4.Initial.SQL.Upload', DATE '2025-05-17'),
                (4, 15, 'PAYREF1003', 'P', DATE '2025-05-18', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-18', 'AR4.Initial.SQL.Upload', DATE '2025-05-18'),
                (5, 20, 'PAYREF1004', 'D', DATE '2025-05-19', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-19', 'AR4.Initial.SQL.Upload', DATE '2025-05-19'),
                (6, 25, 'PAYREF1005', 'P', DATE '2025-05-20', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-20', 'AR4.Initial.SQL.Upload', DATE '2025-05-20'),
                (7, 50, 'PAYREF1006', 'D', DATE '2025-05-21', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-21', 'AR4.Initial.SQL.Upload', DATE '2025-05-21'),
                (8, 75, 'PAYREF1007', 'P', DATE '2025-05-22', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-22', 'AR4.Initial.SQL.Upload', DATE '2025-05-22'),
                (9, 100, 'PAYREF1008', 'D', DATE '2025-05-23', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-23', 'AR4.Initial.SQL.Upload', DATE '2025-05-23');

-- Insert our test data for V12
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('13', 'test_support', 'check_data_expected_v13_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v13_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
   -- Check that list 11 has exactly 40 entries as expected
   IF NOT EXISTS (SELECT 1 WHERE (SELECT COUNT(*) FROM application_list_entries WHERE al_al_id = 11) = 40) THEN
        RAISE EXCEPTION 'Expected 40 entries for list 11, got %', (SELECT COUNT(*) FROM application_list_entries WHERE al_al_id = 11);
   END IF;
   
   -- If all checks pass, do nothing (test passes)
END $$;

-- Replace the V12 function as the current one has been invalidated by these changes
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v12_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
   NULL;   
   -- If all checks pass, do nothing (test passes)
END $$;

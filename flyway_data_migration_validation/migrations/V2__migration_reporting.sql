-- Additional reporting tables and views for data validation and migration verification.

-- Version Control
-- V1.0  	Matthew Harman  26/08/2026	Initial Version
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

DROP TABLE IF EXISTS metadata_exclusion;

CREATE TABLE IF NOT EXISTS metadata_exclusion
(
    exclusion_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    oracle_schema text COLLATE pg_catalog."default" NOT NULL,
    table_name text COLLATE pg_catalog."default" NOT NULL,
    column_name text COLLATE pg_catalog."default" NOT NULL,
    issue text COLLATE pg_catalog."default",
    reason text COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT metadata_exclusion_pkey PRIMARY KEY (exclusion_id)
);

INSERT INTO metadata_exclusion (oracle_schema, table_name, column_name, issue, reason, created_at)
VALUES 
('APPREGISTER', 'APPLICATION_CODES', 'APPLICATION_CODE_END_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'APPLICATION_CODES', 'APPLICATION_CODE_START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'APP_LIST_ENTRY_FEE_ID', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APP_LIST_ENTRY_FEE_STATUS', 'ALEFS_CHANGED_BY', 'type_mismatch', 'ALEFS_CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APP_LIST_ENTRY_OFFICIAL', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APP_LIST_ENTRY_RESOLUTIONS', 'ID', 'missing_in_oracle_snapshot', 'ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APP_LIST_ENTRY_RESOLUTIONS', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APPLICATION_REGISTER', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APPLICATION_LIST_ENTRIES', 'DELETE_DATE', 'missing_in_oracle_snapshot', 'Delete_Date is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LIST_ENTRIES', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APPLICATION_LIST_ENTRIES', 'DELETE_BY', 'missing_in_oracle_snapshot', 'Delete_By is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LIST_ENTRIES', 'ID', 'missing_in_oracle_snapshot', 'ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LIST_ENTRIES', 'IS_DELETED', 'missing_in_oracle_snapshot', 'Is_Deleted is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'APPLICATION_LIST_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'APPLICATION_LIST_TIME', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL time', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'APPLICATION_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'DELETE_DATE', 'missing_in_oracle_snapshot', 'Delete_Date is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'CHILD_DELETED', 'missing_in_oracle_snapshot', 'Child_Deleted is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'DELETE_BY', 'missing_in_oracle_snapshot', 'Delete_By is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'IS_DELETED', 'missing_in_oracle_snapshot', 'Is_Deleted is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'APPLICATION_LISTS', 'ID', 'missing_in_oracle_snapshot', 'ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'APPREG_USER_MAPPING', 'MODERN_CHANGED_BY', 'type_mismatch', 'Deliberately changed from Oracle varchar(73) to PostgreSQL text', NOW()),
('APPREGISTER', 'FEE', 'FEE_START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'FEE', 'FEE_END_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'FEE', 'IS_OFFSITE', 'missing_in_oracle_snapshot', 'IS_OFFSITE is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'NATIONAL_COURT_HOUSES', 'END_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'NATIONAL_COURT_HOUSES', 'START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'RESOLUTION_CODES', 'RESOLUTION_CODE_START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'RESOLUTION_CODES', 'RESOLUTION_CODE_END_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'STANDARD_APPLICANTS', 'STANDARD_APPLICANT_START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'STANDARD_APPLICANTS', 'STANDARD_APPLICANT_END_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'FORENAME_3', 'missing_in_pg', 'Forename_3 is removed from PostgreSQL by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'FORENAME_2', 'missing_in_pg', 'Forename_2 is removed from PostgreSQL by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'FORENAME_1', 'missing_in_pg', 'Forename_1 is removed from PostgreSQL by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'SURNAME', 'missing_in_pg', 'Surname is removed from PostgreSQL by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'FIRST_NAME', 'missing_in_oracle_snapshot', 'First_Name is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'MIDDLE_NAME', 'missing_in_oracle_snapshot', 'Middle_Name is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'LAST_NAME', 'missing_in_oracle_snapshot', 'Last_Name is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'START_DATE', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'CHANGED_BY', 'type_mismatch', 'CHANGED_BY deliberately changed from Oracle numeric to PostgreSQL varchar', NOW()),
('APPREGISTER', 'NAME_ADDRESS', 'DATE_OF_BIRTH', 'type_mismatch', 'Deliberately changed from Oracle timestamp to PostgreSQL date', NOW())
;

DROP TABLE IF EXISTS migration_run;

CREATE TABLE IF NOT EXISTS migration_run
(
    run_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    run_name date NOT NULL,
    started_at timestamp with time zone NOT NULL DEFAULT now(),
    completed_at timestamp with time zone,
    status text COLLATE pg_catalog."default" NOT NULL DEFAULT 'RUNNING'::text,
    CONSTRAINT migration_run_pkey PRIMARY KEY (run_id)
);

DROP TABLE IF EXISTS postgres_column_analysis;

CREATE TABLE IF NOT EXISTS postgres_column_analysis
(
    schema_name text COLLATE pg_catalog."default" NOT NULL,
    table_name text COLLATE pg_catalog."default" NOT NULL,
    column_name text COLLATE pg_catalog."default" NOT NULL,
    metric text COLLATE pg_catalog."default" NOT NULL,
    metric_value text COLLATE pg_catalog."default",
    analysis_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS reconciliation_result;

CREATE TABLE IF NOT EXISTS reconciliation_result
(
    result_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    run_id bigint NOT NULL,
    entity_name text COLLATE pg_catalog."default" NOT NULL,
    check_type text COLLATE pg_catalog."default" NOT NULL,
    source_value text COLLATE pg_catalog."default",
    expected_value text COLLATE pg_catalog."default",
    target_value text COLLATE pg_catalog."default",
    difference text COLLATE pg_catalog."default",
    status text COLLATE pg_catalog."default" NOT NULL,
    executed_at timestamp with time zone NOT NULL DEFAULT now(),
    notes text COLLATE pg_catalog."default",
    CONSTRAINT reconciliation_result_pkey PRIMARY KEY (result_id),
    CONSTRAINT reconciliation_result_run_id_fkey FOREIGN KEY (run_id)
        REFERENCES migration_run (run_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

DROP TABLE IF EXISTS summary;

CREATE TABLE IF NOT EXISTS summary
(
    pg_schema text COLLATE pg_catalog."default",
    pg_table text COLLATE pg_catalog."default",
    oracle_count bigint,
    postgres_count bigint,
    count_diff bigint,
    bucket_issues bigint
);

DROP TABLE IF EXISTS validation_exclusion;

CREATE TABLE IF NOT EXISTS validation_exclusion
(
    oracle_schema text COLLATE pg_catalog."default" NOT NULL,
    table_name text COLLATE pg_catalog."default" NOT NULL,
    column_name text COLLATE pg_catalog."default" NOT NULL,
    reason text COLLATE pg_catalog."default",
    CONSTRAINT validation_exclusion_pkey PRIMARY KEY (oracle_schema, table_name, column_name)
);

INSERT INTO validation_exclusion (oracle_schema, table_name, column_name, reason)
VALUES
('APPREGISTER','APP_LIST_ENTRY_FEE_ID','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APP_LIST_ENTRY_FEE_STATUS','ALEFS_CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APP_LIST_ENTRY_OFFICIAL','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APP_LIST_ENTRY_RESOLUTIONS','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APPLICATION_LIST_ENTRIES','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APPLICATION_LISTS','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','APPLICATION_REGISTER','CHANGED_BY','Intentional source/target structural change'),
('APPREGISTER','NAME_ADDRESS','CHANGED_BY','Intentional source/target structural change')
;

DROP VIEW IF EXISTS column_analysis_comparison;

CREATE OR REPLACE VIEW column_analysis_comparison
 AS
 SELECT o.owner AS oracle_schema,
    o.table_name,
    o.column_name,
    o.metric,
    TRIM(BOTH FROM o.metric_value) AS oracle_value,
    TRIM(BOTH FROM p.metric_value) AS postgres_value,
    c.data_type AS postgres_data_type,
        CASE
            WHEN p.metric_value IS NULL THEN 'FAIL'::text
            WHEN o.metric = 'avg_len'::text THEN
            CASE
                WHEN round(TRIM(BOTH FROM o.metric_value)::numeric, 10) = round(TRIM(BOTH FROM p.metric_value)::numeric, 10) THEN 'PASS'::text
                ELSE 'FAIL'::text
            END
            WHEN (o.metric = ANY (ARRAY['min'::text, 'max'::text])) AND (c.data_type::text = ANY (ARRAY['date'::character varying, 'timestamp without time zone'::character varying, 'timestamp with time zone'::character varying]::text[])) THEN
            CASE
                WHEN TRIM(BOTH FROM o.metric_value)::timestamp without time zone = TRIM(BOTH FROM p.metric_value)::timestamp without time zone THEN 'PASS'::text
                ELSE 'FAIL'::text
            END
            WHEN (o.metric = ANY (ARRAY['min'::text, 'max'::text])) AND (c.data_type::text = ANY (ARRAY['smallint'::character varying, 'integer'::character varying, 'bigint'::character varying, 'numeric'::character varying, 'decimal'::character varying, 'real'::character varying, 'double precision'::character varying]::text[])) THEN
            CASE
                WHEN TRIM(BOTH FROM o.metric_value)::numeric = TRIM(BOTH FROM p.metric_value)::numeric THEN 'PASS'::text
                ELSE 'FAIL'::text
            END
            WHEN o.metric = ANY (ARRAY['null_count'::text, 'distinct_count'::text, 'min_len'::text, 'max_len'::text, 'sum_len'::text]) THEN
            CASE
                WHEN TRIM(BOTH FROM o.metric_value)::numeric = TRIM(BOTH FROM p.metric_value)::numeric THEN 'PASS'::text
                ELSE 'FAIL'::text
            END
            WHEN TRIM(BOTH FROM o.metric_value) = TRIM(BOTH FROM p.metric_value) THEN 'PASS'::text
            ELSE 'FAIL'::text
        END AS result
   FROM ${flyway:defaultSchema}.oracle_column_analysis o
     LEFT JOIN ${flyway:defaultSchema}.postgres_column_analysis p ON upper(p.table_name) = upper(o.table_name) AND upper(p.column_name) = upper(o.column_name) AND lower(p.metric) = lower(o.metric)
     LEFT JOIN ${flyway:defaultSchema}.oracle_table_map m ON upper(m.pg_table) = upper(o.table_name)
     LEFT JOIN information_schema.columns c ON c.table_schema::name = m.pg_schema AND c.table_name::name = m.pg_table AND upper(c.column_name::text) = upper(o.column_name);

DROP VIEW IF EXISTS metadata_comparison;

CREATE OR REPLACE VIEW metadata_comparison
 AS
 SELECT table_name,
    column_name,
    issue,
    details,
    oracle_suggested_pg_type,
    pg_declared_type,
    oracle_char_len,
    pg_char_len,
    oracle_nullable,
    pg_nullable
   FROM ${flyway:defaultSchema}.v_metadata v
  WHERE NOT (EXISTS ( SELECT 1
           FROM ${flyway:defaultSchema}.metadata_exclusion e
          WHERE upper(e.table_name) = upper(v.table_name) AND upper(e.column_name) = upper(v.column_name) AND (e.issue IS NULL OR upper(e.issue) = upper(v.issue))));

CREATE OR REPLACE FUNCTION create_report(
	)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$


DECLARE 
	r record; 
	vsql text;

	l_run_id ${flyway:defaultSchema}.migration_run.run_id%TYPE;
	l_oracle_row_count ${flyway:defaultSchema}.oracle_rowcounts.row_count%TYPE;
	l_expected_value ${flyway:defaultSchema}.oracle_rowcounts.row_count%TYPE;
	l_postgres_row_count ${flyway:defaultSchema}.pg_rowcounts.row_count%TYPE;

	l_status ${flyway:defaultSchema}.reconciliation_result.status%TYPE;

    l_started_at ${flyway:defaultSchema}.migration_run.started_at%TYPE;
    l_completed_at ${flyway:defaultSchema}.migration_run.completed_at%TYPE;
    l_overall_status text;
BEGIN	
	-- create a new report
	insert into ${flyway:defaultSchema}.migration_run(run_name) values(NOW())
	   returning run_id into l_run_id;

    perform ${flyway:defaultSchema}.refresh_pg_counts();
	perform ${flyway:defaultSchema}.refresh_pg_buckets();
	perform ${flyway:defaultSchema}.refresh_pg_column_analysis();
	perform ${flyway:defaultSchema}.refresh_summary();

    -- populate reconciliation result
	FOR r IN SELECT * FROM ${flyway:defaultSchema}.oracle_table_map WHERE ENABLED LOOP
		-- Find the relevant Oracle record
		BEGIN
			SELECT row_count INTO l_oracle_row_count
				FROM ${flyway:defaultSchema}.oracle_rowcounts
				WHERE owner = r.oracle_owner
				AND table_name = r.oracle_table;
		EXCEPTION
			WHEN NO_DATA_FOUND THEN
				-- default it to 0
				l_oracle_row_count = 0;
		END;
		l_expected_value = l_oracle_row_count;
		
		-- Find the relevant Postgres record
		BEGIN
			SELECT row_count INTO l_postgres_row_count
				FROM ${flyway:defaultSchema}.pg_rowcounts
				WHERE pg_schema = r.pg_schema
				AND pg_table = r.pg_table;
		EXCEPTION
			WHEN NO_DATA_FOUND THEN
				-- default it to 0
				l_postgres_row_count = 0;
		END;

		IF l_postgres_row_count = l_expected_value THEN
			l_status = 'PASS';
		ELSE
			l_status = 'FAIL';
		END IF;

		-- write the record
		INSERT INTO ${flyway:defaultSchema}.reconciliation_result(run_id, entity_name, check_type, source_value, expected_value, target_value, difference, status, notes)
		   VALUES(l_run_id, r.oracle_table, 'ROW_COUNT', l_oracle_row_count, l_expected_value, l_postgres_row_count, l_expected_value - l_postgres_row_count, l_status, '');


	END LOOP;
    
    perform ${flyway:defaultSchema}.generate_postgres_column_analysis();

	INSERT INTO ${flyway:defaultSchema}.reconciliation_result
        (run_id, entity_name, check_type, source_value, expected_value, target_value, status, executed_at)
SELECT l_run_id, c.table_name, c.column_name || ':' || upper(c.metric), oracle_value, oracle_value, postgres_value, result, now()
FROM ${flyway:defaultSchema}.column_analysis_comparison c
WHERE NOT EXISTS
(
    SELECT 1
    FROM ${flyway:defaultSchema}.validation_exclusion e
    WHERE upper(e.oracle_schema) = upper(c.oracle_schema)
      AND upper(e.table_name)    = upper(c.table_name)
      AND upper(e.column_name)   = upper(c.column_name)
);

	INSERT INTO ${flyway:defaultSchema}.reconciliation_result
        (run_id, entity_name, check_type, source_value, expected_value, target_value, status, executed_at)
SELECT l_run_id, table_name, column_name || ':COLUMN_STRUCTURE', oracle_suggested_pg_type, oracle_suggested_pg_type, pg_declared_type, 'FAIL', now()
FROM ${flyway:defaultSchema}.metadata_comparison;

	INSERT INTO ${flyway:defaultSchema}.reconciliation_result
        (run_id, entity_name, check_type, source_value, expected_value, target_value, difference, status, executed_at)
SELECT l_run_id, upper(pg_table), 'BUCKET:' || bucket_label, oracle_rows, oracle_rows, postgres_rows, bucket_diff, CASE WHEN bucket_diff = 0 THEN 'PASS' ELSE 'FAIL' END, now()
FROM ${flyway:defaultSchema}.v_bucket_diff;

    SELECT started_at, NOW()
    INTO l_started_at, l_completed_at
    FROM ${flyway:defaultSchema}.migration_run
    WHERE run_id = l_run_id;

    -- output the run report
    RAISE NOTICE '----------------------------------------------------------------------------------------------------------------------';
    RAISE NOTICE 'Comparison report for data migration';
    RAISE NOTICE 'Started at %', l_started_at;
    RAISE NOTICE 'Completed at %', l_completed_at;
    RAISE NOTICE '----------------------------------------------------------------------------------------------------------------------';

    RAISE NOTICE 'Table Counts checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type = 'ROW_COUNT'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type = 'ROW_COUNT' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type = 'ROW_COUNT' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, Distinct Count checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:DISTINCT_COUNT'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:DISTINCT_COUNT' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:DISTINCT_COUNT' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, MAX checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, MIN checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, NULL Count checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:NULL_COUNT'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:NULL_COUNT' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:NULL_COUNT' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, AVG_LEN checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:AVG_LEN'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:AVG_LEN' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:AVG_LEN' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, MAX_LEN checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX_LEN'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX_LEN' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MAX_LEN' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, MIN_LEN checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN_LEN'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN_LEN' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:MIN_LEN' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Analysis, SUM_LEN checked: %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:SUM_LEN'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:SUM_LEN' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:SUM_LEN' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Column Structure, % have incorrect column structure, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:COLUMN_STRUCTURE'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE '%:COLUMN_STRUCTURE' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    RAISE NOTICE 'Bucket Counts checked %, total matched %, Result: %', (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE 'BUCKET:%'), (SELECT count(*) FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE 'BUCKET:%' AND status = 'PASS'), (SELECT CASE WHEN EXISTS(SELECT 1 FROM ${flyway:defaultSchema}.reconciliation_result WHERE run_id = l_run_id AND check_type LIKE 'BUCKET:%' AND status = 'FAIL') THEN 'FAIL' ELSE 'PASS' END);
    
    SELECT CASE
           WHEN EXISTS (
               SELECT 1
               FROM ${flyway:defaultSchema}.reconciliation_result
               WHERE run_id = l_run_id
                 AND status = 'FAIL'
           )
           THEN 'FAIL'
           ELSE 'PASS'
       END
    INTO l_overall_status;

    UPDATE ${flyway:defaultSchema}.migration_run
    SET completed_at = now(),
        status = l_overall_status
    WHERE run_id = l_run_id;

    RAISE NOTICE '----------------------------------------------------------------------------------------------------------------------';
    RAISE NOTICE 'OVERALL MIGRATION VALIDATION RESULT: %', l_overall_status;   
    RAISE NOTICE '----------------------------------------------------------------------------------------------------------------------';

END
$BODY$;

CREATE OR REPLACE FUNCTION generate_postgres_column_analysis(
	)
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
    r_table  record;
    r_metric record;

    v_sql    text;
    v_value  text;
BEGIN
    /*
     * Clear previous analysis.
     *
     * Later, replace this with a run_id so that historical
     * migration runs can be retained.
     */
    TRUNCATE TABLE ${flyway:defaultSchema}.postgres_column_analysis;
	
    /*
     * Process every PostgreSQL table registered in oracle_table_map.
     */
    FOR r_table IN
        SELECT DISTINCT
               pg_schema,
               pg_table
        FROM ${flyway:defaultSchema}.oracle_table_map
        WHERE pg_schema IS NOT NULL
          AND pg_table  IS NOT NULL
        ORDER BY pg_schema, pg_table
    LOOP


        /*
         * Find the Oracle metrics that need to be reproduced
         * against this PostgreSQL table.
         *
         * Assumption:
         * Oracle table_name corresponds to pg_table after
         * removing an optional _temp suffix.
         */
        FOR r_metric IN
            SELECT DISTINCT
                   o.column_name,
				   c.column_name as pg_column_name,
                   o.metric,
				   c.data_type
            FROM ${flyway:defaultSchema}.oracle_column_analysis o
			JOIN information_schema.columns c
			ON c.table_schema = r_table.pg_schema
			AND c.table_name = r_table.pg_table
			AND upper(c.column_name) = upper(o.column_name)
            AND upper(o.table_name) =
                  upper(regexp_replace(
                      r_table.pg_table,
                      '_temp$',
                      '',
                      'i'
                  ))
            ORDER BY o.column_name, o.metric
        LOOP

            /*
             * Generate the appropriate PostgreSQL expression
             * for each Oracle metric.
             */
            CASE lower(r_metric.metric)

                WHEN 'null_count' THEN

                    v_sql := format(
                        'SELECT (COUNT(*) - COUNT(%I))::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );


                WHEN 'distinct_count' THEN

                    v_sql := format(
                        'SELECT COUNT(DISTINCT %I)::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );

      			/* ----------------------------------------
                 * MIN / MAX
                 * ---------------------------------------- */
WHEN 'min', 'max' THEN

    /*
     * r_metric.metric is controlled by the CASE above,
     * so this becomes either MIN or MAX.
     */

    IF r_metric.data_type in ('date','timestamp without time zone') THEN

        v_sql := format(
            $sql$
            SELECT TO_CHAR(
                %s(%I),
                'YYYY-MM-DD HH24:MI:SS'
            )
            FROM %I.%I
            $sql$,
            upper(r_metric.metric),
            r_metric.pg_column_name,
            r_table.pg_schema,
            r_table.pg_table
        );


    ELSIF r_metric.data_type = 'timestamp with time zone' THEN

        v_sql := format(
            $sql$
            SELECT TO_CHAR(
                %s(%I),
                'YYYY-MM-DD HH24:MI:SS'
            )
            FROM %I.%I
            $sql$,
            upper(r_metric.metric),
            r_metric.pg_column_name,
            r_table.pg_schema,
            r_table.pg_table
        );



    ELSE

        v_sql := format(
            $sql$
            SELECT %s(%I)::text
            FROM %I.%I
            $sql$,
            upper(r_metric.metric),
            r_metric.pg_column_name,
            r_table.pg_schema,
            r_table.pg_table
        );

    END IF;

                WHEN 'min_len' THEN

                    v_sql := format(
                        'SELECT COALESCE(MIN(LENGTH(%I::text)), 0)::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );


                WHEN 'max_len' THEN

                    v_sql := format(
                        'SELECT COALESCE(MAX(LENGTH(%I::text)), 0)::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );


                WHEN 'avg_len' THEN

                    v_sql := format(
                        'SELECT COALESCE(AVG(LENGTH(%I::text)), 0)::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );


                WHEN 'sum_len' THEN

                    v_sql := format(
                        'SELECT COALESCE(SUM(LENGTH(%I::text)), 0)::text
                           FROM %I.%I',
                        lower(r_metric.pg_column_name),
                        r_table.pg_schema,
                        r_table.pg_table
                    );


                ELSE

                    RAISE WARNING
                        'Unknown metric "%" for %.%.%',
                        r_metric.metric,
                        r_table.pg_schema,
                        r_table.pg_table,
                        r_metric.pg_column_name;

                    CONTINUE;

            END CASE;


            /*
             * Execute the dynamically generated query.
             */
            EXECUTE v_sql
            INTO v_value;


            /*
             * Store the result.
             *
             * Store column/table names uppercase so they line
             * up conveniently with the Oracle analysis.
             */
            INSERT INTO ${flyway:defaultSchema}.postgres_column_analysis
            (
                schema_name,
                table_name,
                column_name,
                metric,
                metric_value,
                analysis_date
            )
            VALUES
            (
                upper(r_table.pg_schema),
                upper(regexp_replace(
                    r_table.pg_table,
                    '_temp$',
                    '',
                    'i'
                )),
                upper(r_metric.column_name),
                lower(r_metric.metric),
                v_value,
                current_timestamp
            );

        END LOOP;

    END LOOP;

END;
$BODY$;


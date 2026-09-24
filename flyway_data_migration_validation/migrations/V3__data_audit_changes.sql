-- Additional changes for data_audit migration, ARCPOC-1826

-- Version Control
-- V1.0  	Matthew Harman  22/09/2026	Initial Version
--
--

INSERT INTO oracle_table_map(oracle_owner,oracle_table,pg_schema,pg_table,pk_column,time_column)
VALUES
('APPREGISTER','DATA_AUDIT','appreg','data_audit','data_id','created_date');


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

		IF l_postgres_row_count IS NOT DISTINCT FROM l_expected_value THEN
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
            WHEN o.metric_value IS NULL AND p.metric_value IS NULL THEN 'PASS'::text
            WHEN o.metric_value IS NULL OR p.metric_value IS NULL THEN 'FAIL'::text
            WHEN o.metric = 'avg_len'::text THEN
            CASE
                WHEN round(TRIM(BOTH FROM o.metric_value)::numeric, 10) 
                   = round(TRIM(BOTH FROM p.metric_value)::numeric, 10) 
                THEN 'PASS'::text
                ELSE 'FAIL'::text
            END
            WHEN (o.metric = ANY (ARRAY['min'::text, 'max'::text])) 
                 AND (c.data_type::text = ANY (
                    ARRAY[
                        'date'::text,
                        'timestamp without time zone'::text, 
                        'timestamp with time zone'::text
                    ]
                )) 
            THEN
                CASE
                    WHEN TRIM(BOTH FROM o.metric_value)::timestamp without time zone 
                       = TRIM(BOTH FROM p.metric_value)::timestamp without time zone 
                    THEN 'PASS'::text
                    ELSE 'FAIL'::text
                END
            WHEN (o.metric = ANY (ARRAY['min'::text, 'max'::text])) 
                AND (c.data_type::text = ANY (
                    ARRAY[
                        'smallint'::text, 
                        'integer'::text, 
                        'bigint'::text, 
                        'numeric'::text, 
                        'decimal'::text, 
                        'real'::text, 
                        'double precision'::text
                    ]
                )) 
            THEN
                CASE
                    WHEN TRIM(BOTH FROM o.metric_value)::numeric 
                       = TRIM(BOTH FROM p.metric_value)::numeric 
                    THEN 'PASS'::text
                    ELSE 'FAIL'::text
                END
            WHEN o.metric = ANY (
                ARRAY[
                    'null_count'::text, 
                    'distinct_count'::text, 
                    'min_len'::text, 
                    'max_len'::text, 
                    'sum_len'::text
                ]
            ) 
            THEN
                CASE
                    WHEN TRIM(BOTH FROM o.metric_value)::numeric 
                       = TRIM(BOTH FROM p.metric_value)::numeric 
                    THEN 'PASS'::text
                    ELSE 'FAIL'::text
                END
            WHEN TRIM(BOTH FROM o.metric_value) 
               = TRIM(BOTH FROM p.metric_value) 
            THEN 'PASS'::text
            ELSE 'FAIL'::text
        END AS result
   FROM ${flyway:defaultSchema}.oracle_column_analysis o
     LEFT JOIN ${flyway:defaultSchema}.postgres_column_analysis p ON upper(p.table_name) = upper(o.table_name) AND upper(p.column_name) = upper(o.column_name) AND lower(p.metric) = lower(o.metric)
     LEFT JOIN ${flyway:defaultSchema}.oracle_table_map m ON upper(m.pg_table) = upper(o.table_name)
     LEFT JOIN information_schema.columns c ON c.table_schema::name = m.pg_schema AND c.table_name::name = m.pg_table AND upper(c.column_name::text) = upper(o.column_name);

INSERT INTO metadata_exclusion (oracle_schema, table_name, column_name, issue, reason, created_at)
VALUES 
('APPREGISTER', 'DATA_AUDIT', 'USER_ID', 'char_length_mismatch', 'Length deliberately increased from Oracle to PostgreSQL', NOW())
;

INSERT INTO validation_exclusion (oracle_schema, table_name, column_name, reason)
VALUES
('APPREGISTER','DATA_AUDIT','USER_ID','Intentional source/target structural change')
;

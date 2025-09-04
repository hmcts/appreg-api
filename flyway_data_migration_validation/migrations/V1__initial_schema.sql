-- Add tables and code to enable data_validation

-- Version Control
-- V1.0  	Matthew Harman  29/08/2025	Initial Version
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

CREATE TABLE oracle_column_metadata (
	owner text,
	table_name text,
	column_name text,
	data_type text,
	char_length integer,
	nullable text,
	suggested_pg_type text,
    loaded_at timestamptz default now()
) ;

CREATE TABLE oracle_rowcounts (
	owner text,
	table_name text,
	row_count bigint,
	loaded_at timestamptz default now()
) ;

CREATE TABLE oracle_counts_by_date (
	owner text,
	table_name text,
	bucket_label text,
	row_count bigint,
	loaded_at timestamptz default now()
) ;

CREATE TABLE oracle_column_analysis (
	owner text,
	table_name text,
	column_name text,
	metric text,
	metric_value text,
	loaded_at timestamptz default now()
) ;

CREATE TABLE oracle_table_map (
	oracle_owner text NOT NULL,
	oracle_table text NOT NULL,
	pg_schema text NOT NULL,
	pg_table text NOT NULL,
	pk_column text,
	time_column text,
	bucket_count integer DEFAULT 16,
	enabled boolean DEFAULT true,
	PRIMARY KEY (oracle_owner, oracle_table)
);

INSERT INTO oracle_table_map(oracle_owner,oracle_table,pg_schema,pg_table,pk_column,time_column)
VALUES
('APPREGISTER','APPLICATION_CODES','appreg','application_codes','ac_id','changed_date'),
('APPREGISTER','APPLICATION_LISTS','appreg','application_lists','al_id','changed_date'),
('APPREGISTER','APPLICATION_LIST_ENTRIES','appreg','application_list_entries','ale_id','changed_date'),
('APPREGISTER','APPLICATION_REGISTER','appreg','application_register','ar_id','changed_date'),
('APPREGISTER','APP_LIST_ENTRY_FEE_ID','appreg','app_list_entry_fee_id','ale_ale_id','changed_date'),
('APPREGISTER','APP_LIST_ENTRY_FEE_STATUS','appreg','app_list_entry_fee_status','alefs_id','alefs_changed_date'),
('APPREGISTER','APP_LIST_ENTRY_OFFICIAL','appreg','app_list_entry_official','aleo_id','changed_date'),
('APPREGISTER','APP_LIST_ENTRY_RESOLUTIONS','appreg','app_list_entry_resolutions','aler_id','changed_date'),
('APPREGISTER','CRIMINAL_JUSTICE_AREA','appreg','criminal_justice_area','cja_id',NULL),
('APPREGISTER','FEE','appreg','fee','fee_id','fee_changed_date'),
('APPREGISTER','NAME_ADDRESS','appreg','name_address','na_id','changed_date'),
('APPREGISTER','RESOLUTION_CODES','appreg','resolution_codes','rc_id','changed_date'),
('APPREGISTER','STANDARD_APPLICANTS','appreg','standard_applicants','sa_id','changed_date'),
('LIBRA','NATIONAL_COURT_HOUSES','appreg','national_court_houses','nch_id','changed_date'),
('LIBRA','LINK_ADDRESSES','appreg','link_addresses','la_id','changed_date'),
('LIBRA','ADDRESSES','appreg','addresses','adr_id','changed_date'),
('LIBRA','LINK_COMMUNICATION_MEDIA','appreg','link_communication_media','lcm_id','changed_date'),
('LIBRA','COMMUNICATION_MEDIA','appreg','communication_media','comm_id','changed_date'),
('LIBRA','PETTY_SESSIONAL_AREAS','appreg','petty_sessional_areas','psa_id','changed_date');

CREATE VIEW v_oracle_counts AS
SELECT 	lower(m.pg_schema)	AS pg_schema,
       	lower(m.pg_table)	AS pg_table,
		c.row_count,
		c.loaded_at
FROM oracle_rowcounts c
JOIN oracle_table_map m
ON lower(m.oracle_owner)=lower(c.owner)
AND lower(m.oracle_table)=lower(c.table_name)
WHERE m.enabled;

CREATE VIEW v_oracle_buckets AS
SELECT lower(m.pg_schema) AS pg_schema,
       lower(m.pg_table) AS pg_table,
	   b.bucket_label,
	   b.row_count,
	   b.loaded_at
FROM oracle_counts_by_date b
JOIN oracle_table_map m
ON LOWER(m.oracle_owner) = lower(b.owner)
AND lower(m.oracle_table) = lower(b.table_name)
WHERE m.enabled;

CREATE VIEW v_oracle_columns AS
SELECT lower(m.pg_schema) AS pg_schema,
       lower(m.pg_table) AS pg_table,
	   lower(cp.column_name) AS column_name,
	   lower(cp.metric) AS metric,
	   cp.metric_value,
	   cp.loaded_at
FROM oracle_column_analysis cp
JOIN oracle_table_map m
ON lower(m.oracle_owner) = lower(cp.owner)
AND lower(m.oracle_table) = lower(cp.table_name)
WHERE m.enabled;

CREATE TABLE pg_rowcounts (
	pg_schema text,
	pg_table text,
	row_count bigint,
	loaded_at timestamptz DEFAULT NOW(),
	PRIMARY KEY (pg_schema, pg_table)
);

CREATE TABLE pg_counts_by_date (
	pg_schema text,
	pg_table text,
	bucket_label text,
	row_count bigint,
	loaded_at timestamptz DEFAULT NOW(),
	PRIMARY KEY (pg_schema, pg_table, bucket_label)
);

CREATE TABLE pg_column_analysis(
	pg_schema text,
	pg_table text,
	column_name text,
	metric text,
	metric_value text,
	created_at timestamptz DEFAULT now(),
	PRIMARY KEY (pg_schema, pg_table, column_name, metric)
);


CREATE OR REPLACE FUNCTION refresh_pg_counts() RETURNS void LANGUAGE plpgsql AS $$
DECLARE r record; vsql text; vcnt bigint;
BEGIN
	FOR r IN
		SELECT * FROM data_validation.oracle_table_map WHERE enabled
	LOOP
		vsql := format('SELECT COUNT(*) FROM %I.%I', r.pg_schema, r.pg_table);
		EXECUTE vsql INTO vcnt;

		INSERT INTO data_validation.pg_rowcounts(pg_schema, pg_table, row_count)
			VALUES (lower(r.pg_schema), lower(r.pg_table), vcnt)
			ON CONFLICT (pg_schema, pg_table)
			DO UPDATE SET row_count=EXCLUDED.row_count, loaded_at=now();
	END LOOP;
END$$;

CREATE OR REPLACE FUNCTION refresh_pg_buckets() RETURNS void LANGUAGE plpgsql AS $$
DECLARE r record; vsql text;
BEGIN	
	-- clear and refill per refresh for simplicity
	DELETE FROM data_validation.pg_counts_by_date;

	FOR r IN SELECT * FROM data_validation.oracle_table_map WHERE ENABLED LOOP
		IF r.time_column IS NOT NULL THEN
			vsql := format($f$
				INSERT INTO data_validation.pg_counts_by_date(pg_schema, pg_table, bucket_label, row_count)
				SELECT %L, %L,
					to_char(date_trunc('day', %I),  'YYYY-MM-DD') AS bucket_label,
					count(*)::bigint
				FROM %I.%I
				GROUP BY 1,2,3
				$f$, lower(r.pg_schema), lower(r.pg_table), r.time_column, r.pg_schema, r.pg_table);
		ELSE
			vsql := format($f$
				INSERT INTO data_validation.pg_counts_by_date(pg_schema, pg_table, bucket_label, row_count)
				SELECT %L, %L, 'bucket all', count(*)::bigint
				FROM %I.%I
				$f$, lower(r.pg_schema), lower(r.pg_table), r.pg_schema, r.pg_table);
		END IF;

		EXECUTE vsql;
	END LOOP;
END$$;

CREATE OR REPLACE FUNCTION refresh_pg_column_analysis() RETURNS void LANGUAGE plpgsql AS $$
DECLARE
	r 		record;
	c		record;
	v_min	text;
	v_max	text;
	vtext	text;
BEGIN
	DELETE FROM data_validation.pg_column_analysis;

	FOR r IN SELECT * FROM data_validation.oracle_table_map WHERE enabled LOOP
		-- per column metrics (intentionally simple)
		FOR c IN
			SELECT column_name, data_type
			FROM information_schema.columns
			WHERE table_schema=lower(r.pg_schema) AND table_name=lower(r.pg_table)
		LOOP
			-- min/max: attempt for every column (except char ones); ignore if type doesn't support it
			BEGIN
				IF c.data_type IN ('character varying','character','text','citext') THEN
					v_min := NULL;
					v_max := NULL;
				ELSE	
					EXECUTE format('SELECT min(%1$I)::text FROM %2$I.%3$I WHERE %1$I IS NOT NULL', c.column_name, r.pg_schema, r.pg_table) INTO v_min;
					EXECUTE format('SELECT max(%1$I)::text FROM %2$I.%3$I WHERE %1$I IS NOT NULL', c.column_name, r.pg_schema, r.pg_table) INTO v_max;
				END IF;
				IF v_min IS NOT NULL THEN
					INSERT INTO data_validation.pg_column_analysis
						(pg_schema, pg_table, column_name, metric, metric_value) 
					VALUES (lower(r.pg_schema), lower(r.pg_table), lower(c.column_name), 'min', v_min)
					ON CONFLICT (pg_schema, pg_table, column_name, metric) 
					DO UPDATE SET metric_value=EXCLUDED.metric_value, created_at=now();
				END IF;

				IF v_max IS NOT NULL THEN
					INSERT INTO data_validation.pg_column_analysis
						(pg_schema, pg_table, column_name, metric, metric_value) 
					VALUES (lower(r.pg_schema), lower(r.pg_table), lower(c.column_name), 'max', v_max)
					ON CONFLICT (pg_schema, pg_table, column_name, metric) 
					DO UPDATE SET metric_value=EXCLUDED.metric_value, created_at=now();
				END IF;

			EXCEPTION WHEN undefined_function OR sqlstate '42883' THEN 
				-- Type has no min/max aggregate or cast - skip quietly
				v_min := NULL; v_max := NULL;
			END;

			-- avg_len for character-ish/text 
			-- Oracle: '' == NULL for VARCHAR2, so AVG(LENGTH(col)) ignores ''.
			IF c.data_type IN ('character varying','character','text','citext') THEN
				EXECUTE format('SELECT COALESCE(avg(length(NULLIF(%1$I, ''''))),0)::text FROM %2$I.%3$I', c.column_name, r.pg_schema, r.pg_table) INTO vtext;
				INSERT INTO data_validation.pg_column_analysis VALUES (lower(r.pg_schema), lower(r.pg_table), lower(c.column_name), 'avg_len', COALESCE(vtext,'0'))
				ON CONFLICT (pg_schema, pg_table, column_name, metric) DO UPDATE SET metric_value=EXCLUDED.metric_value, created_at=now();
			END IF;
		END LOOP;
	END LOOP;
END$$;

CREATE VIEW v_count_diff AS
SELECT 
	o.pg_schema, o.pg_table,
	o.row_count AS oracle_count,
	p.row_count AS postgres_count,
	(p.row_count - o.row_count) AS count_diff,
	o.loaded_at AS oracle_loaded_at,
	p.loaded_at AS postgres_loaded_at
FROM v_oracle_counts o
JOIN pg_rowcounts p
ON p.pg_schema=o.pg_schema AND p.pg_table=o.pg_table
ORDER BY abs(p.row_count - o.row_count) DESC, o.pg_schema, o.pg_table;

CREATE VIEW v_bucket_diff AS
SELECT 
	o.pg_schema, o.pg_table, o.bucket_label,
	o.row_count AS oracle_rows,
	p.row_count AS postgres_rows,
	(p.row_count - o.row_count) AS bucket_diff
FROM v_oracle_buckets o
JOIN pg_counts_by_date P
ON p.pg_schema=o.pg_schema AND p.pg_table=o.pg_table AND p.bucket_label=o.bucket_label
ORDER BY o.pg_schema, o.pg_table, o.bucket_label;

CREATE VIEW v_minmax_diff AS
WITH o AS (
		SELECT pg_schema, pg_table, column_name,
			   max(metric_value) FILTER (WHERE metric='min') AS o_min,
		       max(metric_value) FILTER (WHERE metric='max') AS o_max
FROM v_oracle_columns
WHERE metric IN ('min','max')
GROUP BY pg_schema, pg_table, column_name
),
p AS (
	SELECT pg_schema, pg_table, column_name,
		   max(metric_value) FILTER (WHERE metric='min') AS p_min,
		   max(metric_value) FILTER (WHERE metric='max') AS p_max
FROM pg_column_analysis
WHERE metric IN ('min','max')
GROUP BY pg_schema, pg_table, column_name
)
SELECT 
	coalesce(p.pg_schema, o.pg_schema)	AS pg_schema,
	coalesce(p.pg_table, o.pg_table)	AS pg_table,
	coalesce(p.column_name, o.column_name) AS column_name,
	o.o_min, p.p_min,
	o.o_max, p.p_max
FROM o
FULL JOIN p
ON p.pg_schema=o.pg_schema AND p.pg_table=o.pg_table AND p.column_name=o.column_name
WHERE (o.o_min IS DISTINCT FROM p.p_min)
OR (o.o_max IS DISTINCT FROM p.p_max)
ORDER BY pg_schema, pg_table, column_name;


CREATE VIEW v_avglen_diff AS
SELECT 
	o.pg_schema, o.pg_table, o.column_name,
	round(p.metric_value::numeric,2) AS pg_avg_len,
	round(o.metric_value::numeric,2) AS ora_avg_len,
	round((p.metric_value::numeric - o.metric_value::numeric),2) as avglen_diff
FROM v_oracle_columns o
JOIN pg_column_analysis p
ON p.pg_schema=o.pg_schema AND p.pg_table=o.pg_table
AND p.column_name=o.column_name
WHERE o.metric='avg_len' AND p.metric='avg_len'
ORDER BY abs((p.metric_value::numeric - o.metric_value::numeric)) DESC;

CREATE TABLE summary AS
SELECT 
	c.pg_schema, c.pg_table,
	c.oracle_count, c.postgres_count, c.count_diff,
	coalesce((SELECT count(*) FROM v_bucket_diff b
		WHERE b.pg_schema=c.pg_schema AND b.pg_table=c.pg_table AND b.bucket_diff<>0),0) AS bucket_issues
FROM v_count_diff c
WITH NO DATA;

CREATE OR REPLACE FUNCTION refresh_summary() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	TRUNCATE data_validation.summary;
	INSERT INTO data_validation.summary
		SELECT 
			c.pg_schema, c.pg_table,
			c.oracle_count, c.postgres_count, c.count_diff,
			coalesce((SELECT count(*) FROM data_validation.v_bucket_diff b
				WHERE b.pg_schema=c.pg_schema AND b.pg_table=c.pg_table AND b.bucket_diff<>0),0)
		FROM data_validation.v_count_diff c;
END$$;

create view v_metadata
as 
-- ORACLE DATA STRUCTURE
-- Compare only for tables present in oracle_colmeta_min
(WITH owner_map AS (
  -- Map as many owners → PG schemas as you need
  SELECT 'libra'::text       AS oracle_owner_norm, 'appreg'::text AS pg_schema_norm
  UNION ALL
  SELECT 'appregister'::text AS oracle_owner_norm, 'appreg'::text AS pg_schema_norm
),
-- Oracle snapshot (normalized)
normed_oracle AS (
  SELECT
    m.pg_schema_norm,
    lower(btrim(regexp_replace(table_name,'^"|"$','','g')))  AS table_name_norm,
    lower(btrim(regexp_replace(column_name,'^"|"$','','g'))) AS column_name_norm,
    upper(data_type) AS oracle_data_type,
    char_length,
    CASE WHEN nullable IN ('Y','y') THEN 'YES' ELSE 'NO' END AS oracle_nullable,
    lower(btrim(regexp_replace(coalesce(suggested_pg_type,''),'^"|"$','','g'))) AS suggested_pg_type_norm
  FROM data_Validation.oracle_column_metadata o
  JOIN owner_map m
    ON lower(btrim(regexp_replace(o.owner,'^"|"$','','g'))) = m.oracle_owner_norm
),
-- The set of tables we care about (from Oracle snapshot)
oracle_tables AS (
  SELECT DISTINCT pg_schema_norm, table_name_norm FROM normed_oracle
),
-- Live PG columns, but ONLY for tables that exist in oracle_tables; also exclude *_temp
normed_pg AS (
  SELECT
    lower(c.table_schema) AS pg_schema_norm,
    lower(c.table_name)   AS table_name_norm,
    lower(c.column_name)  AS column_name_norm,
    c.is_nullable,
    c.character_maximum_length,
    CASE
      WHEN c.data_type IN ('character varying','character') AND c.character_maximum_length IS NOT NULL
        THEN 'character varying(' || c.character_maximum_length || ')'
      WHEN c.data_type IN ('character varying','character') AND c.character_maximum_length IS NULL
        THEN 'character varying'
      WHEN c.data_type IN ('text','citext') THEN 'text'
      WHEN c.data_type IN ('smallint','integer','bigint') THEN c.data_type
      WHEN c.data_type IN ('numeric','decimal') THEN 'numeric'
      WHEN c.data_type IN ('timestamp without time zone','timestamp with time zone') THEN c.data_type
      WHEN c.data_type = 'date' THEN 'date'
      ELSE c.data_type
    END AS pg_declared_type_norm,
    CASE WHEN c.data_type IN ('character varying','character')
         THEN c.character_maximum_length END AS pg_char_len
  FROM information_schema.columns c
  JOIN owner_map m ON lower(c.table_schema) = m.pg_schema_norm
  JOIN oracle_tables ot
    ON lower(c.table_schema) = ot.pg_schema_norm
   AND lower(c.table_name)   = ot.table_name_norm
  WHERE lower(c.table_name) NOT LIKE '%_temp'
),
-- Missing columns (now only within the Oracle table set)
missing AS (
  SELECT p.table_name_norm AS table_name, p.column_name_norm AS column_name,
         'missing_in_oracle_snapshot' AS issue,
         'Exists in PG but not in Oracle snapshot' AS details
  FROM normed_pg p
  LEFT JOIN normed_oracle o
    ON o.pg_schema_norm  = p.pg_schema_norm
   AND o.table_name_norm = p.table_name_norm
   AND o.column_name_norm= p.column_name_norm
  WHERE o.column_name_norm IS NULL
  UNION ALL
  SELECT o.table_name_norm AS table_name, o.column_name_norm AS column_name,
         'missing_in_pg' AS issue,
         'Exists in Oracle snapshot but not in PG' AS details
  FROM normed_oracle o
  LEFT JOIN normed_pg p
    ON p.pg_schema_norm  = o.pg_schema_norm
   AND p.table_name_norm = o.table_name_norm
   AND p.column_name_norm= o.column_name_norm
  WHERE p.column_name_norm IS NULL
),
-- Mismatches (type/length/nullability)
mismatch_core AS (
  SELECT
    o.table_name_norm AS table_name,
    o.column_name_norm AS column_name,
    CASE
      WHEN o.suggested_pg_type_norm LIKE 'character varying(%'
           AND p.pg_declared_type_norm LIKE 'character varying(%'
           AND COALESCE(o.char_length, -1) <> COALESCE(p.pg_char_len, -1)
        THEN 'char_length_mismatch'
      WHEN o.suggested_pg_type_norm = 'numeric'
           AND p.pg_declared_type_norm NOT IN ('numeric','smallint','integer','bigint')
        THEN 'type_mismatch'
      WHEN o.suggested_pg_type_norm = 'smallint'
           AND p.pg_declared_type_norm <> 'smallint'
        THEN 'type_mismatch'
      WHEN o.suggested_pg_type_norm LIKE 'character varying(%'
           AND p.pg_declared_type_norm <> o.suggested_pg_type_norm
        THEN 'type_mismatch'
      WHEN o.suggested_pg_type_norm = 'timestamp without time zone'
           AND p.pg_declared_type_norm <> 'timestamp without time zone'
        THEN 'type_mismatch'
    END AS type_issue,
    CASE WHEN p.is_nullable IS DISTINCT FROM o.oracle_nullable
         THEN 'nullability_mismatch' END AS nullability_issue,
    o.suggested_pg_type_norm AS oracle_suggested_pg_type,
    p.pg_declared_type_norm  AS pg_declared_type,
    o.char_length            AS oracle_char_len,
    p.pg_char_len            AS pg_char_len,
    o.oracle_nullable        AS oracle_nullable,
    p.is_nullable            AS pg_nullable
  FROM normed_oracle o
  JOIN normed_pg p
    ON p.pg_schema_norm   = o.pg_schema_norm
   AND p.table_name_norm  = o.table_name_norm
   AND p.column_name_norm = o.column_name_norm
)
-- Final report
SELECT *
FROM (
  SELECT table_name, column_name, issue, details,
         NULL::text AS oracle_suggested_pg_type, NULL::text AS pg_declared_type,
         NULL::int  AS oracle_char_len, NULL::int AS pg_char_len,
         NULL::text AS oracle_nullable, NULL::text AS pg_nullable
  FROM missing
  UNION ALL
  SELECT table_name, column_name,
         COALESCE(type_issue, nullability_issue) AS issue,
         NULL::text AS details,
         oracle_suggested_pg_type, pg_declared_type,
         oracle_char_len, pg_char_len,
         oracle_nullable, pg_nullable
  FROM mismatch_core
  WHERE type_issue IS NOT NULL OR nullability_issue IS NOT NULL
) z
ORDER BY table_name, column_name, issue
);

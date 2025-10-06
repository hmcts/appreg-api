-- V8__set_changed_by_to_varchar.sql
-- Change all columns named changed_by to varchar(73)
-- This is to accommodate the user being identified by a combination of OID and TID.
-- TID - Tenent ID: 36 characters long.
-- OID - Object ID: 36 characters long.
-- Seperator ':' - 1 character long.
BEGIN;

-- 1) Remember dependent views (and their definitions)
CREATE TEMP TABLE tmp_dep_views (
  schema_name text,
  view_name   text,
  definition  text,
  is_mat      boolean
) ON COMMIT DROP;

INSERT INTO tmp_dep_views(schema_name, view_name, definition, is_mat)
SELECT
  n.nspname                         AS schema_name,
  c.relname                         AS view_name,
  pg_get_viewdef(c.oid, true)       AS definition,  -- just the SELECT body
  (c.relkind = 'm')                 AS is_mat       -- 'm' = materialized view
FROM pg_depend d
JOIN pg_rewrite r     ON d.objid = r.oid
JOIN pg_class   c     ON r.ev_class = c.oid
JOIN pg_namespace n   ON c.relnamespace = n.oid
WHERE c.relkind IN ('v','m')
  AND d.refobjid IN (
      SELECT pc.oid
      FROM information_schema.columns ic
      JOIN pg_class pc
        ON pc.relname = ic.table_name
      JOIN pg_namespace pn
        ON pn.oid = pc.relnamespace
       AND pn.nspname = ic.table_schema
      WHERE ic.column_name LIKE '%changed_by'
        AND ic.table_schema NOT IN ('pg_catalog','information_schema')
  );

-- 2) Drop those views (CASCADE to handle dependency chains)
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM tmp_dep_views LOOP
    IF r.is_mat THEN
      EXECUTE format('DROP MATERIALIZED VIEW IF EXISTS %I.%I CASCADE;', r.schema_name, r.view_name);
    ELSE
      EXECUTE format('DROP VIEW IF EXISTS %I.%I CASCADE;', r.schema_name, r.view_name);
    END IF;
  END LOOP;
END $$;

-- 3) Convert all %changed_by columns to varchar(73) (with safety check)
DO $$
DECLARE
  rec     record;
  maxlen  integer;
  curtype text;
BEGIN
  FOR rec IN
    SELECT table_schema, table_name, column_name
    FROM information_schema.columns
    WHERE column_name LIKE '%changed_by'
      AND table_schema NOT IN ('pg_catalog','information_schema')
  LOOP
    -- Skip if already varchar(73)
    SELECT data_type || COALESCE('('||character_maximum_length||')','')
      INTO curtype
    FROM information_schema.columns
    WHERE table_schema = rec.table_schema
      AND table_name   = rec.table_name
      AND column_name  = rec.column_name;

    IF curtype ILIKE 'character varying(73)' THEN
      CONTINUE;
    END IF;

    -- Ensure we won't truncate anything
    EXECUTE format('SELECT max(length(%I::text)) FROM %I.%I',
                   rec.column_name, rec.table_schema, rec.table_name)
      INTO maxlen;
    IF maxlen IS NULL THEN maxlen := 0; END IF;
    IF maxlen > 73 THEN
      RAISE EXCEPTION 'Cannot alter %.%.% to varchar(73): existing max length is %',
        rec.table_schema, rec.table_name, rec.column_name, maxlen;
    END IF;

    -- Perform the alteration
    EXECUTE format(
      'ALTER TABLE %I.%I
         ALTER COLUMN %I TYPE varchar(73)
         USING %I::varchar(73);',
      rec.table_schema, rec.table_name, rec.column_name, rec.column_name
    );
  END LOOP;
END $$;

-- 4) Recreate the saved views
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM tmp_dep_views LOOP
    IF r.is_mat THEN
      EXECUTE format('CREATE MATERIALIZED VIEW %I.%I AS %s WITH NO DATA;', r.schema_name, r.view_name, r.definition);
      -- Optionally refresh during tests:
      -- EXECUTE format('REFRESH MATERIALIZED VIEW %I.%I;', r.schema_name, r.view_name);
    ELSE
      EXECUTE format('CREATE OR REPLACE VIEW %I.%I AS %s;', r.schema_name, r.view_name, r.definition);
    END IF;
  END LOOP;
END $$;

COMMIT;

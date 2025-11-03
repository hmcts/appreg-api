DO $$
DECLARE
    latest text;
    ran_count int := 0;
    missing_count int := 0;
    missing_list text;
    r record;
BEGIN
    -- Determine the latest successfully applied migration version
    SELECT version INTO latest 
    FROM flyway_schema_history 
    WHERE success = true
    ORDER BY installed_rank DESC 
    LIMIT 1;

    IF latest IS NULL THEN
        RAISE EXCEPTION 'No successful versioned migrations found in flyway_schema_history.';
    END IF;

    -- Enforce: every migrated version must have at least one registered test.
    -- This makes forgotten/omitted test registrations fail immediately.
    WITH migrated_versions AS (
        SELECT DISTINCT version
        FROM flyway_schema_history
        WHERE success = true
        AND version IS NOT NULL
    ),
    coverage AS (
        SELECT m.version,
            EXISTS (
                SELECT 1
                FROM test_support.test_registry t
                WHERE t.version = m.version
            ) AS has_tests
        FROM migrated_versions m
    )
    SELECT COUNT(*) AS missing,
        string_agg(version, ', ' ORDER BY version) AS missing_list
        INTO missing_count, missing_list
        FROM coverage
        WHERE has_tests = FALSE;
    
    IF missing_count > 0 THEN
        RAISE EXCEPTION 'No tests registered for migrated versions(s): %. Add rows to test_support.test_registry for these versions.',
        missing_list;
    END IF;
    
    -- Execute all registered test routines with version <= latest.
    FOR r IN
        SELECT version, routine_schema, routine_name
        FROM test_support.test_registry
        WHERE test_support.version_le(version, latest)
        ORDER BY string_to_array(version, '.')::int[]
    LOOP
        RAISE NOTICE ' -> %.%() (v%)', r.routine_schema, r.routine_name, r.version;
        EXECUTE format('SELECT %I.%I()', r.routine_schema, r.routine_name);
        ran_count := ran_count + 1;
    END LOOP;

    -- Hard stop if nothing ran (guards against empty registry/mismatch).
    IF ran_count = 0 THEN
        RAISE EXCEPTION 'No tests executed. Check test_support.test_registry contents and version formatting.';
    END IF;
END $$;
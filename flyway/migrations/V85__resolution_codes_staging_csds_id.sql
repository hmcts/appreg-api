-- Keep the staging backup compatible with resolution_codes after V82.
ALTER TABLE resolution_codes_staging ADD csds_id integer;

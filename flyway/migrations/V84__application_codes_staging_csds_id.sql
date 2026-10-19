-- Keep the staging backup compatible with application_codes after V82.
ALTER TABLE application_codes_staging ADD csds_id integer;

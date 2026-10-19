-- Keep the staging backup compatible with standard_applicants after V82.
ALTER TABLE standard_applicants_staging ADD csds_id integer;

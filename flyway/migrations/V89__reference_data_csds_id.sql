-- Some existing environments already have these nullable identity columns.
ALTER TABLE application_codes ADD COLUMN IF NOT EXISTS csds_id integer;
ALTER TABLE resolution_codes ADD COLUMN IF NOT EXISTS csds_id integer;
ALTER TABLE standard_applicants ADD COLUMN IF NOT EXISTS csds_id integer;
ALTER TABLE national_court_houses ADD COLUMN IF NOT EXISTS csds_id integer;
ALTER TABLE fee ADD COLUMN IF NOT EXISTS csds_id integer;

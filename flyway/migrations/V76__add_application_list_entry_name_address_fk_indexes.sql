-- Support the applicant/respondent replacement flow by indexing the FK columns
-- that PostgreSQL checks when old name_address rows are deleted.
CREATE INDEX IF NOT EXISTS ale_a_na_id_idx ON application_list_entries (a_na_id);

-- The respondent FK needs the same treatment for the equivalent delete path.
CREATE INDEX IF NOT EXISTS ale_r_na_id_idx ON application_list_entries (r_na_id);

ALTER TABLE application_list_entries DROP CONSTRAINT application_list_entries_is_deleted_check;

update application_list_entries set is_deleted = 'Y' where is_deleted = '1';
update application_list_entries set is_deleted = 'N' where is_deleted = '0';

ALTER TABLE application_list_entries ALTER COLUMN is_deleted SET DEFAULT 'N';
ALTER TABLE application_list_entries ADD CONSTRAINT application_list_entries_is_deleted_check CHECK (is_deleted = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]));

ALTER TABLE application_lists DROP CONSTRAINT application_lists_is_deleted_check;

update application_lists set is_deleted = 'Y' where is_deleted = '1';
update application_lists set is_deleted = 'N' where is_deleted = '0';

ALTER TABLE application_lists ALTER COLUMN is_deleted SET DEFAULT 'N';
ALTER TABLE application_lists ADD CONSTRAINT application_lists_is_deleted_check CHECK (is_deleted = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]));

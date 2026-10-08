-- v80__run_CSDS_dup_and_reset_keys.sql 

-- Version Control
-- V1.0  	Matthew Harman  08/10/2026	Initial Version
--

call ${flyway:defaultSchema}.remove_CSDS_duplication();

alter table application_list_entries drop constraint ale_ac_fk;   

alter table application_codes add constraint application_codes_ac_id_key unique (ac_id);

alter table application_codes drop constraint application_codes_pk;

alter table application_codes add constraint application_codes_pk primary key (application_code);

alter table application_list_entries add constraint ale_ac_fk foreign key (ac_ac_id) references application_codes(ac_id);

drop index if exists ac_application_code_idx;

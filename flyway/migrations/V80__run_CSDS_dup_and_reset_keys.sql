-- v80__run_CSDS_dup_and_reset_keys.sql 

-- Version Control
-- V1.0  	Matthew Harman  08/10/2026	Initial Version
--

-- application_codes table
call ${flyway:defaultSchema}.remove_CSDS_duplication_ac();

alter table application_list_entries drop constraint ale_ac_fk;   

alter table application_codes add constraint application_codes_ac_id_key unique (ac_id);

alter table application_codes drop constraint application_codes_pk;

alter table application_codes add constraint application_codes_pk primary key (application_code);

alter table application_list_entries add constraint ale_ac_fk foreign key (ac_ac_id) references application_codes(ac_id);

drop index if exists ac_application_code_idx;

-- resolution_codes table
call ${flyway:defaultSchema}.remove_CSDS_duplication_rc();

alter table app_list_entry_resolutions drop constraint aler_rc_fk;   

alter table resolution_codes add constraint resolution_codes_rc_id_key unique (rc_id);

alter table resolution_codes drop constraint resolution_codes_pk;

alter table resolution_codes add constraint resolution_codes_pk primary key (resolution_code);

alter table app_list_entry_resolutions add constraint aler_rc_fk foreign key (rc_rc_id) references resolution_codes(rc_id);

-- fee table
call ${flyway:defaultSchema}.remove_CSDS_duplication_fee();

alter table app_list_entry_fee_id drop constraint alefi_fee_fk;   

alter table fee add constraint fee_id_key unique (fee_id);

alter table fee drop constraint fee_id_pk;

alter table fee add constraint fee_pk primary key (fee_reference, fee_start_date);

alter table app_list_entry_fee_id add constraint alefi_fee_fk foreign key (fee_fee_id) references fee(fee_id);

-- nch table
call ${flyway:defaultSchema}.remove_CSDS_duplication_nch();

alter table national_court_houses add constraint nch_id_key unique (nch_id);

alter table national_court_houses drop constraint nch_pk;

alter table national_court_houses add constraint nch_pk primary key (courthouse_name);


-- standard_applicants table
call ${flyway:defaultSchema}.remove_CSDS_duplication_sa();

alter table application_list_entries drop constraint ale_sa_fk;   

alter table standard_applicants add constraint standard_applicants_sa_id_key unique (sa_id);

alter table standard_applicants drop constraint standard_applicant_pk;

alter table standard_applicants add constraint standard_applicant_pk primary key (standard_applicant_code);

alter table application_list_entries add constraint ale_sa_fk foreign key (sa_sa_id) references standard_applicants(sa_id);

drop index if exists sa_sac_idx;


-- Version History
--
-- v1.0 initial version
-- v1.1 added drop trigger as it fires on loading causing erroneous data

alter table appreg.application_lists drop constraint al_cja_fk;
alter table appreg.application_list_entries drop constraint ale_sa_fk;
alter table appreg.application_list_entries drop constraint ale_r_na_fk;
alter table appreg.application_list_entries drop constraint ale_a_na_fk;
alter table appreg.app_list_entry_fee_id drop constraint alefi_fee_fk;
alter table appreg.app_list_entry_resolutions drop constraint aler_rc_fk;
alter table appreg.national_court_houses drop constraint nch_psa_fk;
alter table appreg.link_communication_media drop constraint lcm_comm_fk;

alter table appreg.addresses disable trigger adr_version_trg;

alter table appreg.communication_media disable trigger comm_version_trg;

alter table appreg.link_addresses disable trigger la_version_trg;

alter table appreg.link_communication_media disable trigger lcm_version_trg;

alter table appreg.petty_sessional_areas disable trigger psa_version_trg;

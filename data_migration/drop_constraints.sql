alter table appreg.application_lists drop constraint al_cja_fk;
alter table appreg.application_list_entries drop constraint ale_sa_fk;
alter table appreg.application_list_entries drop constraint ale_r_na_fk;
alter table appreg.application_list_entries drop constraint ale_a_na_fk;
alter table appreg.app_list_entry_fee_id drop constraint alefi_fee_fk;
alter table appreg.app_list_entry_resolutions drop constraint aler_rc_fk;
alter table appreg.national_court_houses drop constraint nch_psa_fk;
alter table appreg.link_communication_media drop constraint lcm_comm_fk;


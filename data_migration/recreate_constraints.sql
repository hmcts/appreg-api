ALTER TABLE appreg.application_lists ADD CONSTRAINT al_cja_fk FOREIGN KEY (cja_cja_id) REFERENCES appreg.criminal_justice_area(cja_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.application_list_entries ADD CONSTRAINT ale_a_na_fk FOREIGN KEY (a_na_id) REFERENCES appreg.name_address(na_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.application_list_entries ADD CONSTRAINT ale_r_na_fk FOREIGN KEY (r_na_id) REFERENCES appreg.name_address(na_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.application_list_entries ADD CONSTRAINT ale_sa_fk FOREIGN KEY (sa_sa_id) REFERENCES appreg.standard_applicants(sa_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.app_list_entry_fee_id ADD CONSTRAINT alefi_fee_fk FOREIGN KEY (fee_fee_id) REFERENCES appreg.fee(fee_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.app_list_entry_resolutions ADD CONSTRAINT aler_rc_fk FOREIGN KEY (rc_rc_id) REFERENCES appreg.resolution_codes(rc_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.national_court_houses ADD CONSTRAINT nch_psa_fk FOREIGN KEY (psa_psa_id) REFERENCES appreg.petty_sessional_areas(psa_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE appreg.link_communication_media ADD CONSTRAINT lcm_comm_fk FOREIGN KEY (comm_comm_id) REFERENCES appreg.communication_media(comm_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

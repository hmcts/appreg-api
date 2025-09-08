-- Initial Creation script for Appregister

-- Version Control
-- V1.0  	Matthew Harman  31/07/2025	Initial Version
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;


CREATE TABLE addresses (
	adr_id bigint NOT NULL,
	line1 varchar(35),
	line2 varchar(35),
	line3 varchar(35),
	line4 varchar(35),
	line5 varchar(35),
	postcode varchar(8),
	start_date timestamp NOT NULL,
	end_date timestamp,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	mcc_mcc_id bigint
) ;
CREATE INDEX a_mcc_upp_line1_i ON addresses (mcc_mcc_id, upper(line1));
CREATE INDEX a_mcc_upp_postcode_line1_i ON addresses (mcc_mcc_id, upper(replace(postcode,' ','')), upper(line1));
ALTER TABLE addresses ADD CONSTRAINT adr_pk PRIMARY KEY (adr_id);
ALTER TABLE addresses ADD CONSTRAINT adr_date_range_chk CHECK (
/* start_date not null and end date >= start date or null
 and both are dates with 00:00 time */
start_date = date_trunc('day', start_date) AND ( coalesce(end_date::text, '') = '' OR ( end_date = date_trunc('day', end_date)
AND end_date >= start_date
)
));

CREATE TABLE application_codes (
	ac_id NUMERIC NOT NULL,
	application_code varchar(10) NOT NULL,
	application_code_title varchar(500) NOT NULL,
	application_code_wording text NOT NULL,
	application_legislation text,
	fee_due char(1) NOT NULL,
	application_code_respondent char(1) NOT NULL,
	ac_destination_email_address_1 varchar(253),
	ac_destination_email_address_2 varchar(253),
	application_code_start_date timestamp NOT NULL,
	application_code_end_date timestamp,
	bulk_respondent_allowed char(1) NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250),
	ac_fee_reference varchar(12)
) ;
CREATE INDEX ac_application_code_idx ON application_codes (application_code);
CREATE INDEX ac_fee_reference_idx ON application_codes (ac_fee_reference);
ALTER TABLE application_codes ADD CONSTRAINT application_codes_pk PRIMARY KEY (ac_id);

CREATE TABLE application_lists (
	al_id NUMERIC NOT NULL,
	application_list_status varchar(6),
	application_list_date timestamp NOT NULL,
	application_list_time timestamp NOT NULL,
	courthouse_code varchar(10),
	other_courthouse varchar(200),
	list_description varchar(200) NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250),
	courthouse_name varchar(200),
	duration_hour smallint,
	duration_minute smallint,
	cja_cja_id NUMERIC
) ;
CREATE INDEX al_ald_idx ON application_lists (application_list_date);
CREATE INDEX al_cja_idx ON application_lists (cja_cja_id);
CREATE INDEX al_idx ON application_lists (courthouse_code, application_list_date);
CREATE INDEX al_upp_ld_idx ON application_lists (upper(list_description));
CREATE INDEX al_upp_oc_idx ON application_lists (upper(other_courthouse));
ALTER TABLE application_lists ADD CONSTRAINT application_lists_pk PRIMARY KEY (al_id);

CREATE TABLE application_list_entries (
	ale_id NUMERIC NOT NULL,
	al_al_id NUMERIC NOT NULL,
	sa_sa_id NUMERIC,
	ac_ac_id NUMERIC NOT NULL,
	a_na_id NUMERIC,
	r_na_id NUMERIC,
	number_of_bulk_respondents smallint,
	application_list_entry_wording text NOT NULL,
	case_reference varchar(15),
	account_number varchar(20),
	entry_rescheduled char(1) NOT NULL,
	notes varchar(4000),
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	bulk_upload varchar(1),
	user_name varchar(250),
	sequence_number smallint NOT NULL,
	tcep_status varchar(2),
	message_uuid varchar(36),
	retry_count varchar(36),
	lodgement_date timestamp NOT NULL
) ;
ALTER TABLE application_list_entries ADD CONSTRAINT application_list_entries_pk PRIMARY KEY (ale_id);

CREATE TABLE application_register (
	ar_id NUMERIC NOT NULL,
	al_al_id NUMERIC NOT NULL,
	text text,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp with time zone NOT NULL,
	user_name varchar(250)
) ;
CREATE INDEX ale_al_al_id_idx ON application_register (al_al_id);
ALTER TABLE application_register ADD CONSTRAINT application_register_pk PRIMARY KEY (ar_id);

CREATE TABLE app_list_entry_fee_id (
	ale_ale_id NUMERIC NOT NULL,
	fee_fee_id NUMERIC NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250) NOT NULL
) ;
ALTER TABLE app_list_entry_fee_id ADD CONSTRAINT alefi_pk PRIMARY KEY (ale_ale_id,fee_fee_id);

CREATE TABLE app_list_entry_fee_status (
	alefs_id NUMERIC NOT NULL,
	alefs_ale_id NUMERIC NOT NULL,
	alefs_payment_reference varchar(15),
	alefs_fee_status varchar(1) NOT NULL,
	alefs_fee_status_date timestamp NOT NULL,
	alefs_version NUMERIC NOT NULL,
	alefs_changed_by NUMERIC NOT NULL,
	alefs_changed_date timestamp NOT NULL,
	alefs_user_name varchar(250) NOT NULL,
	alefs_status_creation_date timestamp
) ;
CREATE INDEX alefs_ale_id_idx ON app_list_entry_fee_status (alefs_ale_id);
ALTER TABLE app_list_entry_fee_status ADD CONSTRAINT alefs_id_pk PRIMARY KEY (alefs_id);
ALTER TABLE app_list_entry_fee_status ADD CONSTRAINT alefs_fee_status_chk CHECK (alefs_fee_status IN ('D','P','R','U')
);

CREATE TABLE app_list_entry_official (
	aleo_id NUMERIC NOT NULL,
	ale_ale_id NUMERIC NOT NULL,
	title varchar(100),
	forename varchar(100),
	surname varchar(100),
	official_type varchar(1) NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250) NOT NULL
) ;
CREATE INDEX ale_ale_idx ON app_list_entry_official (ale_ale_id);
ALTER TABLE app_list_entry_official ADD CONSTRAINT app_list_entry_official_pk PRIMARY KEY (aleo_id);
ALTER TABLE app_list_entry_official ADD CONSTRAINT aleo_official_type_chk CHECK (official_type IN ('C','M'));

CREATE TABLE app_list_entry_resolutions (
	aler_id NUMERIC NOT NULL,
	rc_rc_id NUMERIC NOT NULL,
	ale_ale_id NUMERIC NOT NULL,
 	al_entry_resolution_wording text NOT NULL,
	al_entry_resolution_officer varchar(1000) NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250)
) ;
CREATE INDEX aler_ale_id ON app_list_entry_resolutions (ale_ale_id);
CREATE INDEX aler_rc_rc_id_idx ON app_list_entry_resolutions (rc_rc_id);
ALTER TABLE app_list_entry_resolutions ADD CONSTRAINT app_list_entry_resolutions_pk PRIMARY KEY (aler_id);

CREATE TABLE communication_media (
	comm_id bigint NOT NULL,
	detail varchar(254) NOT NULL,
	start_date timestamp NOT NULL,
	end_date timestamp,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL
) ;
ALTER TABLE communication_media ADD CONSTRAINT comm_pk PRIMARY KEY (comm_id);

CREATE TABLE criminal_justice_area (
	cja_id NUMERIC NOT NULL,
	cja_code varchar(2) NOT NULL,
	cja_description varchar(35) NOT NULL
) ;
CREATE INDEX cja_code_idx ON criminal_justice_area (cja_code);
ALTER TABLE criminal_justice_area ADD CONSTRAINT criminal_justice_area_pk PRIMARY KEY (cja_id);

CREATE TABLE fee (
	fee_id NUMERIC NOT NULL,
	fee_reference varchar(12) NOT NULL,
	fee_description varchar(250) NOT NULL,
	fee_value double precision NOT NULL,
	fee_start_date timestamp NOT NULL,
	fee_end_date timestamp,
	fee_version NUMERIC NOT NULL,
	fee_changed_by NUMERIC NOT NULL,
	fee_changed_date timestamp NOT NULL,
	fee_user_name varchar(250) NOT NULL
);

CREATE INDEX fee_reference_idx ON fee (fee_reference);
ALTER TABLE fee ADD CONSTRAINT fee_id_pk PRIMARY KEY (fee_id);

CREATE TABLE link_addresses (
	la_id bigint NOT NULL,
	no_fixed_abode varchar(1) NOT NULL DEFAULT 'N',
	la_type varchar(5) NOT NULL,
	start_date timestamp NOT NULL,
	end_date timestamp,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	adr_adr_id bigint NOT NULL,
	bu_bu_id bigint,
	er_er_id bigint,
	loc_loc_id bigint,
	head_office_indicator varchar(1)
) ;
CREATE INDEX la_bu_fk_i ON link_addresses (bu_bu_id);
CREATE INDEX la_er_la_type_date_i ON link_addresses (er_er_id, la_type, start_date, end_date);
CREATE INDEX la_loc_fk_i ON link_addresses (loc_loc_id);
ALTER TABLE link_addresses ADD CONSTRAINT la_pk PRIMARY KEY (la_id);
ALTER TABLE link_addresses ADD CONSTRAINT la_date_range_chk CHECK (
/* start_date not null and end date >= start date or null
 and both are dates with 00:00 time */
start_date = date_trunc('day', start_date) AND ( coalesce(end_date::text, '') = '' OR ( end_date = date_trunc('day', end_date)
AND end_date >= start_date
)
));
ALTER TABLE link_addresses ADD CONSTRAINT la_arc CHECK (CASE WHEN coalesce(bu_bu_id::text, '') = '' THEN 0  ELSE 1 END +CASE WHEN coalesce(er_er_id::text, '') = '' THEN 0  ELSE 1 END +CASE WHEN coalesce(loc_loc_id::text, '') = '' THEN 0  ELSE 1 END  = 1);

CREATE TABLE link_communication_media (
	lcm_id bigint NOT NULL,
	lcm_type varchar(2) NOT NULL,
	start_date timestamp,
	end_date timestamp,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	comm_comm_id bigint NOT NULL,
	loc_loc_id bigint,
	er_er_id bigint,
	bu_bu_id bigint
) ;
CREATE INDEX lcm_bu_fk_i ON link_communication_media (bu_bu_id);
CREATE INDEX lcm_er_lcm_type_date_i ON link_communication_media (er_er_id, lcm_type, start_date, end_date);
CREATE INDEX lcm_loc_fk_i ON link_communication_media (loc_loc_id);
ALTER TABLE link_communication_media ADD CONSTRAINT lcm_pk PRIMARY KEY (lcm_id);
ALTER TABLE link_communication_media ADD CONSTRAINT lcm_arc CHECK (CASE WHEN coalesce(bu_bu_id::text, '') = '' THEN 0  ELSE 1 END +CASE WHEN coalesce(er_er_id::text, '') = '' THEN 0  ELSE 1 END +CASE WHEN coalesce(loc_loc_id::text, '') = '' THEN 0  ELSE 1 END  = 1);

CREATE TABLE name_address (
	na_id NUMERIC NOT NULL,
	code varchar(10),
	name varchar(100),
	title varchar(100),
	forename_1 varchar(100),
	forename_2 varchar(100),
	forename_3 varchar(100),
	surname varchar(100),
	address_l1 varchar(35) NOT NULL,
	address_l2 varchar(35),
	address_l3 varchar(35),
	address_l4 varchar(35),
	address_l5 varchar(35),
	postcode varchar(8),
	email_address varchar(253),
	telephone_number varchar(20),
	mobile_number varchar(20),
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250),
	date_of_birth timestamp,
	dms_id varchar(20)
) ;
CREATE INDEX na_upp_n_idx ON name_address (upper(name));
CREATE INDEX na_upp_pc_idx ON name_address (upper(postcode));
CREATE INDEX na_upp_s_idx ON name_address (upper(surname));
ALTER TABLE name_address ADD CONSTRAINT name_address_pk PRIMARY KEY (na_id);

CREATE TABLE national_court_houses (
	nch_id bigint NOT NULL,
	courthouse_name varchar(100) NOT NULL,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	court_type varchar(10) NOT NULL,
	start_date timestamp NOT NULL,
	end_date timestamp,
	loc_loc_id bigint,
	psa_psa_id bigint,
	court_location_code varchar(10),
	sl_courthouse_name varchar(100),
	norg_id bigint
) ;
CREATE INDEX nch_loc_fk_i ON national_court_houses (loc_loc_id);
CREATE INDEX nch_name_court_type_i ON national_court_houses (courthouse_name, court_type);
CREATE INDEX nch_psa_fk_i ON national_court_houses (psa_psa_id);
CREATE INDEX nch_testing_i ON national_court_houses (nch_id, court_location_code);
ALTER TABLE national_court_houses ADD CONSTRAINT nch_pk PRIMARY KEY (nch_id);

CREATE TABLE petty_sessional_areas (
	psa_id bigint NOT NULL,
	psa_name varchar(100),
	short_name varchar(10),
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	cma_cma_id bigint,
	psa_code varchar(4) NOT NULL,
	start_date timestamp,
	end_date timestamp,
	jc_name varchar(200) DEFAULT 'No name entered',
	court_type varchar(10) NOT NULL DEFAULT 'PSA',
	crime_cases_loc_id bigint,
	fine_accounts_loc_id bigint,
	maintenance_enforcement_loc_id bigint,
	family_cases_loc_id bigint,
	court_location_code varchar(10),
	central_finance_loc_id bigint,
	sl_psa_name varchar(100),
	norg_id bigint
) ;
CREATE INDEX psa_cma_fk_i ON petty_sessional_areas (cma_cma_id);
CREATE INDEX psa_crime_cases_loc_fk_i ON petty_sessional_areas (crime_cases_loc_id);
CREATE INDEX psa_family_cases_loc_fk_i ON petty_sessional_areas (family_cases_loc_id);
CREATE INDEX psa_fine_accounts_fk_i ON petty_sessional_areas (fine_accounts_loc_id);
CREATE INDEX psa_maint_enf_loc_fk_i ON petty_sessional_areas (maintenance_enforcement_loc_id);
CREATE INDEX psa_name_court_type_i ON petty_sessional_areas (psa_name, court_type);
ALTER TABLE petty_sessional_areas ADD CONSTRAINT psa_pk PRIMARY KEY (psa_id);
ALTER TABLE petty_sessional_areas ADD CONSTRAINT chk_psa_code CHECK (((psa_code::numeric)  > 0) and (length(psa_code)=4));
ALTER TABLE petty_sessional_areas ADD CONSTRAINT chk_jc_name CHECK ( (court_type IN ('PSA', 'YCT') AND (jc_name IS NOT NULL AND jc_name::text <> '')) OR (court_type NOT IN ('PSA', 'YCT') ));

CREATE TABLE resolution_codes (
	rc_id NUMERIC NOT NULL,
	resolution_code varchar(10) NOT NULL,
	resolution_code_title varchar(500) NOT NULL,
	resolution_code_wording text NOT NULL,
	resolution_legislation text,
	rc_destination_email_address_1 varchar(253),
	rc_destination_email_address_2 varchar(253),
	resolution_code_start_date timestamp NOT NULL,
	resolution_code_end_date timestamp,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250)
) ;
ALTER TABLE resolution_codes ADD CONSTRAINT resolution_codes_pk PRIMARY KEY (rc_id);

CREATE TABLE standard_applicants (
	sa_id NUMERIC NOT NULL,
	standard_applicant_code varchar(10) NOT NULL,
	standard_applicant_start_date timestamp NOT NULL,
	standard_applicant_end_date timestamp,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250),
	name varchar(100),
	title varchar(100),
	forename_1 varchar(100),
	forename_2 varchar(100),
	forename_3 varchar(100),
	surname varchar(100),
	address_l1 varchar(35) NOT NULL,
	address_l2 varchar(35),
	address_l3 varchar(35),
	address_l4 varchar(35),
	address_l5 varchar(35),
	postcode varchar(8),
	email_address varchar(253),
	telephone_number varchar(20),
	mobile_number varchar(20)
) ;
CREATE INDEX sa_sac_idx ON standard_applicants (standard_applicant_code);
CREATE INDEX sa_upp_fn_1_idx ON standard_applicants (upper(forename_1));
CREATE INDEX sa_upp_fn_2_idx ON standard_applicants (upper(forename_2));
CREATE INDEX sa_upp_fn_3_idx ON standard_applicants (upper(forename_3));
CREATE INDEX sa_upp_n_idx ON standard_applicants (upper(name));
CREATE INDEX sa_upp_s_idx ON standard_applicants (upper(surname));
ALTER TABLE standard_applicants ADD CONSTRAINT standard_applicant_pk PRIMARY KEY (sa_id);
ALTER TABLE application_lists ADD CONSTRAINT al_cja_fk FOREIGN KEY (cja_cja_id) REFERENCES criminal_justice_area(cja_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE application_list_entries ADD CONSTRAINT ale_ac_fk FOREIGN KEY (ac_ac_id) REFERENCES application_codes(ac_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE application_list_entries ADD CONSTRAINT ale_al_fk FOREIGN KEY (al_al_id) REFERENCES application_lists(al_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE application_list_entries ADD CONSTRAINT ale_a_na_fk FOREIGN KEY (a_na_id) REFERENCES name_address(na_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE application_list_entries ADD CONSTRAINT ale_r_na_fk FOREIGN KEY (r_na_id) REFERENCES name_address(na_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE application_list_entries ADD CONSTRAINT ale_sa_fk FOREIGN KEY (sa_sa_id) REFERENCES standard_applicants(sa_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE application_register ADD CONSTRAINT ar_al_fk FOREIGN KEY (al_al_id) REFERENCES application_lists(al_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE app_list_entry_fee_id ADD CONSTRAINT alefi_ale_fk FOREIGN KEY (ale_ale_id) REFERENCES application_list_entries(ale_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE app_list_entry_fee_id ADD CONSTRAINT alefi_fee_fk FOREIGN KEY (fee_fee_id) REFERENCES fee(fee_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE app_list_entry_fee_status ADD CONSTRAINT alefs_id_fk FOREIGN KEY (alefs_ale_id) REFERENCES application_list_entries(ale_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE app_list_entry_official ADD CONSTRAINT aleo_id_fk FOREIGN KEY (ale_ale_id) REFERENCES application_list_entries(ale_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE app_list_entry_resolutions ADD CONSTRAINT aler_ale_fk FOREIGN KEY (ale_ale_id) REFERENCES application_list_entries(ale_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE app_list_entry_resolutions ADD CONSTRAINT aler_rc_fk FOREIGN KEY (rc_rc_id) REFERENCES resolution_codes(rc_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE link_communication_media ADD CONSTRAINT lcm_comm_fk FOREIGN KEY (comm_comm_id) REFERENCES communication_media(comm_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE national_court_houses ADD CONSTRAINT nch_psa_fk FOREIGN KEY (psa_psa_id) REFERENCES petty_sessional_areas(psa_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;

-- Generated by Ora2Pg, the Oracle database Schema converter, version 22.1
-- Copyright 2000-2021 Gilles DAROLD. All rights reserved.
-- DATASOURCE: dbi:Oracle:host=10.100.29.14;sid=LDEV;port=1521

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

CREATE OR REPLACE VIEW v_application_codes_noclob (ac_id, application_code, application_code_title, application_code_wording, application_legislation, fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference) AS SELECT AC_ID
,      APPLICATION_CODE
,      APPLICATION_CODE_TITLE
,      substr(APPLICATION_CODE_WORDING,4000,1) application_code_wording
,      substr(APPLICATION_LEGISLATION,4000,1)  application_legislation
,      FEE_DUE
,      APPLICATION_CODE_RESPONDENT
,      AC_DESTINATION_EMAIL_ADDRESS_1
,      AC_DESTINATION_EMAIL_ADDRESS_2
,      APPLICATION_CODE_START_DATE
,      APPLICATION_CODE_END_DATE
,      BULK_RESPONDENT_ALLOWED
,      VERSION
,      CHANGED_BY
,      CHANGED_DATE
,      USER_NAME
,      AC_FEE_REFERENCE
FROM   application_codes;

CREATE OR REPLACE VIEW v_resolution_codes_noclob (rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_legislation, rc_destination_email_address_1, rc_destination_email_address_2, resolution_code_start_date, resolution_code_end_date, version, changed_by, changed_date, user_name) AS SELECT RC_ID
,      RESOLUTION_CODE
,      RESOLUTION_CODE_TITLE
,      substr(RESOLUTION_CODE_WORDING,4000,1)  RESOLUTION_CODE_WORDING
,      substr(RESOLUTION_LEGISLATION,4000,1)   RESOLUTION_LEGISLATION
,      RC_DESTINATION_EMAIL_ADDRESS_1
,      RC_DESTINATION_EMAIL_ADDRESS_2
,      RESOLUTION_CODE_START_DATE
,      RESOLUTION_CODE_END_DATE
,      VERSION
,      CHANGED_BY
,      CHANGED_DATE
,      USER_NAME
FROM   resolution_codes;


-- Generated by Ora2Pg, the Oracle database Schema converter, version 22.1
-- Copyright 2000-2021 Gilles DAROLD. All rights reserved.
-- DATASOURCE: dbi:Oracle:host=10.100.29.14;sid=LDEV;port=1521

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

DROP TRIGGER IF EXISTS adr_version_trg ON addresses CASCADE;
CREATE OR REPLACE FUNCTION trigger_fct_adr_version_trg() RETURNS trigger AS $BODY$
BEGIN
   NEW.version_number := coalesce(OLD.version_number,0) + 1;
RETURN NEW;
END
$BODY$
 LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS adr_version_trg ON addresses;
CREATE TRIGGER adr_version_trg
	BEFORE INSERT ON addresses FOR EACH ROW
	EXECUTE PROCEDURE trigger_fct_adr_version_trg();

DROP TRIGGER IF EXISTS comm_version_trg ON communication_media CASCADE;
CREATE OR REPLACE FUNCTION trigger_fct_comm_version_trg() RETURNS trigger AS $BODY$
BEGIN
   NEW.version_number := coalesce(OLD.version_number,0) + 1;
RETURN NEW;
END
$BODY$
 LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS comm_version_trg ON communication_media;
CREATE TRIGGER comm_version_trg
	BEFORE INSERT ON communication_media FOR EACH ROW
	EXECUTE PROCEDURE trigger_fct_comm_version_trg();

DROP TRIGGER IF EXISTS la_version_trg ON link_addresses CASCADE;
CREATE OR REPLACE FUNCTION trigger_fct_la_version_trg() RETURNS trigger AS $BODY$
BEGIN
   NEW.version_number := coalesce(OLD.version_number,0) + 1;
RETURN NEW;
END
$BODY$
 LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS la_version_trg ON link_addresses;
CREATE TRIGGER la_version_trg
	BEFORE INSERT ON link_addresses FOR EACH ROW
	EXECUTE PROCEDURE trigger_fct_la_version_trg();

DROP TRIGGER IF EXISTS lcm_version_trg ON link_communication_media CASCADE;
CREATE OR REPLACE FUNCTION trigger_fct_lcm_version_trg() RETURNS trigger AS $BODY$
BEGIN
   NEW.version_number := coalesce(OLD.version_number,0) + 1;
RETURN NEW;
END
$BODY$
 LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS lcm_version_trg ON link_communication_media;
CREATE TRIGGER lcm_version_trg
	BEFORE INSERT ON link_communication_media FOR EACH ROW
	EXECUTE PROCEDURE trigger_fct_lcm_version_trg();

DROP TRIGGER IF EXISTS psa_version_trg ON petty_sessional_areas CASCADE;
CREATE OR REPLACE FUNCTION trigger_fct_psa_version_trg() RETURNS trigger AS $BODY$
BEGIN
   NEW.version_number := coalesce(OLD.version_number,0) + 1;
RETURN NEW;
END
$BODY$
 LANGUAGE 'plpgsql';

DROP TRIGGER IF EXISTS psa_version_trg ON petty_sessional_areas;
CREATE TRIGGER psa_version_trg
	BEFORE INSERT ON petty_sessional_areas FOR EACH ROW
	EXECUTE PROCEDURE trigger_fct_psa_version_trg();

-- Generated by Ora2Pg, the Oracle database Schema converter, version 22.1
-- Copyright 2000-2021 Gilles DAROLD. All rights reserved.
-- DATASOURCE: dbi:Oracle:host=10.100.29.14;sid=LDEV;port=1521

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

DROP SEQUENCE IF EXISTS ac_seq;
CREATE SEQUENCE ac_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2502 CACHE 20;
DROP SEQUENCE IF EXISTS adr_seq;
CREATE SEQUENCE adr_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 321364044 CACHE 1000;
DROP SEQUENCE IF EXISTS alefs_seq;
CREATE SEQUENCE alefs_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 4203738 CACHE 20;
DROP SEQUENCE IF EXISTS aleo_seq;
CREATE SEQUENCE aleo_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 5145566 CACHE 20;
DROP SEQUENCE IF EXISTS aler_seq;
CREATE SEQUENCE aler_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2526316 CACHE 20;
DROP SEQUENCE IF EXISTS al_seq;
CREATE SEQUENCE al_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2975601 CACHE 20;
DROP SEQUENCE IF EXISTS ar_seq;
CREATE SEQUENCE ar_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 159880 CACHE 20;
DROP SEQUENCE IF EXISTS cja_seq;
CREATE SEQUENCE cja_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 61 CACHE 20;
DROP SEQUENCE IF EXISTS comm_seq;
CREATE SEQUENCE comm_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 69732262 CACHE 1000;
DROP SEQUENCE IF EXISTS fee_seq;
CREATE SEQUENCE fee_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 221 CACHE 20;
DROP SEQUENCE IF EXISTS la_seq;
CREATE SEQUENCE la_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 113190350 CACHE 1000;
DROP SEQUENCE IF EXISTS lcm_seq;
CREATE SEQUENCE lcm_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 69643755 CACHE 1000;
DROP SEQUENCE IF EXISTS na_seq;
CREATE SEQUENCE na_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2766691 CACHE 20;
DROP SEQUENCE IF EXISTS psa_seq;
CREATE SEQUENCE psa_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 8205 CACHE 20;
DROP SEQUENCE IF EXISTS rc_seq;
CREATE SEQUENCE rc_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 221 CACHE 20;
DROP SEQUENCE IF EXISTS sa_seq;
CREATE SEQUENCE sa_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 9723 CACHE 20;
DROP SEQUENCE IF EXISTS ale_seq;
CREATE SEQUENCE ale_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2975601 CACHE 20;
DROP SEQUENCE IF EXISTS nch_seq;
CREATE SEQUENCE nch_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 9230 CACHE 20;

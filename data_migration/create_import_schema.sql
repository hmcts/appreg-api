DROP TABLE IF EXISTS appreg.application_codes_temp;
DROP TABLE IF EXISTS appreg.application_lists_temp;
DROP TABLE IF EXISTS appreg.application_list_entries_temp;
DROP TABLE IF EXISTS appreg.application_register_temp;
DROP TABLE IF EXISTS appreg.app_list_entry_fee_id_temp;
DROP TABLE IF EXISTS appreg.app_list_entry_fee_status_temp;
DROP TABLE IF EXISTS appreg.app_list_entry_official_temp;
DROP TABLE IF EXISTS appreg.app_list_entry_resolutions_temp;
DROP TABLE IF EXISTS appreg.criminal_justice_area_temp;
DROP TABLE IF EXISTS appreg.fee_temp;
DROP TABLE IF EXISTS appreg.name_address_temp;
DROP TABLE IF EXISTS appreg.resolution_codes_temp;
DROP TABLE IF EXISTS appreg.standard_applicants_temp;
DROP TABLE IF EXISTS appreg.national_court_houses_temp;
DROP TABLE IF EXISTS appreg.link_addresses_temp;
DROP TABLE IF EXISTS appreg.addresses_temp;
DROP TABLE IF EXISTS appreg.link_communication_media_temp;
DROP TABLE IF EXISTS appreg.communication_media_temp;
DROP TABLE IF EXISTS appreg.petty_sessional_areas_temp;

CREATE UNLOGGED TABLE appreg.application_codes_temp (
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
	ac_fee_reference varchar(12),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.application_lists_temp (
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
	cja_cja_id NUMERIC,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.application_list_entries_temp (
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
	lodgement_date timestamp NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.app_list_entry_fee_id_temp (
	ale_ale_id NUMERIC NOT NULL,
	fee_fee_id NUMERIC NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250) NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.app_list_entry_fee_status_temp (
	alefs_id NUMERIC NOT NULL,
	alefs_ale_id NUMERIC NOT NULL,
	alefs_payment_reference varchar(15),
	alefs_fee_status varchar(1) NOT NULL,
	alefs_fee_status_date timestamp NOT NULL,
	alefs_version NUMERIC NOT NULL,
	alefs_changed_by NUMERIC NOT NULL,
	alefs_changed_date timestamp NOT NULL,
	alefs_user_name varchar(250) NOT NULL,
	alefs_status_creation_date timestamp,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.app_list_entry_official_temp (
	aleo_id NUMERIC NOT NULL,
	ale_ale_id NUMERIC NOT NULL,
	title varchar(100),
	forename varchar(100),
	surname varchar(100),
	official_type varchar(1) NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250) NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.app_list_entry_resolutions_temp (
	aler_id NUMERIC NOT NULL,
	rc_rc_id NUMERIC NOT NULL,
	ale_ale_id NUMERIC NOT NULL,
	al_entry_resolution_wording text NOT NULL,
	al_entry_resolution_officer varchar(1000) NOT NULL,
	version NUMERIC NOT NULL,
	changed_by NUMERIC NOT NULL,
	changed_date timestamp NOT NULL,
	user_name varchar(250),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.criminal_justice_area_temp (
	cja_id NUMERIC NOT NULL,
	cja_code varchar(2) NOT NULL,
	cja_description varchar(35) NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.fee_temp (
	fee_id NUMERIC NOT NULL,
	fee_reference varchar(12) NOT NULL,
	fee_description varchar(250) NOT NULL,
	fee_value double precision NOT NULL,
	fee_start_date timestamp NOT NULL,
	fee_end_date timestamp,
	fee_version NUMERIC NOT NULL,
	fee_changed_by NUMERIC NOT NULL,
	fee_changed_date timestamp NOT NULL,
	fee_user_name varchar(250) NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.name_address_temp (
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
	dms_id varchar(20),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.resolution_codes_temp (
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
	user_name varchar(250),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.standard_applicants_temp (
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
	mobile_number varchar(20),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.national_court_houses_temp (
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
	norg_id bigint,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.link_addresses_temp (
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
	head_office_indicator varchar(1),
	marker text
) ;

CREATE UNLOGGED TABLE appreg.addresses_temp (
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
	mcc_mcc_id bigint,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.link_communication_media_temp (
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
	bu_bu_id bigint,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.communication_media_temp (
	comm_id bigint NOT NULL,
	detail varchar(254) NOT NULL,
	start_date timestamp NOT NULL,
	end_date timestamp,
	version_number numeric(38) NOT NULL,
	changed_by bigint NOT NULL,
	changed_date timestamp NOT NULL,
	marker text
) ;

CREATE UNLOGGED TABLE appreg.petty_sessional_areas_temp (
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
	norg_id bigint,
	marker text
) ;

create unlogged table appreg.application_register_temp(
ar_id numeric not null,
al_al_id numeric not null,
changed_by numeric not null,
changed_date_utc text not null,
user_name text,
piece_no integer not null,
piece_escaped text not null,
marker text
 );


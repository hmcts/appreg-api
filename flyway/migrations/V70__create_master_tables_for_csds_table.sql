-- v68__create_master_tables_for_csds_table.sql 

-- Version Control
-- V1.0  	Matthew Harman  15/07/2026	Initial Version
--

CREATE TABLE csds_realignment(
    cr_id NUMERIC NOT NULL,
    legacy_id NUMERIC,
    modern_id NUMERIC,
    notes TEXT NOT NULL
);

CREATE SEQUENCE cr_seq 
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

CREATE TABLE application_codes_master
(
    ac_id numeric NOT NULL,
    application_code character varying(10) NOT NULL,
    application_code_title character varying(500) NOT NULL,
    application_code_wording text NOT NULL,
    application_legislation text,
    fee_due character(1) NOT NULL,
    application_code_respondent character(1) NOT NULL,
    ac_destination_email_address_1 character varying(253),
    ac_destination_email_address_2 character varying(253),
    application_code_start_date date NOT NULL,
    application_code_end_date date,
    bulk_respondent_allowed character(1) NOT NULL,
    version numeric NOT NULL,
    changed_by numeric NOT NULL,
    changed_date timestamp without time zone NOT NULL,
    user_name character varying(250),
    ac_fee_reference character varying(12)
);

CREATE TABLE fee_master (
    fee_id BIGINT NOT NULL,
    fee_reference varchar(12) NOT NULL,
    fee_description character varying(250) NOT NULL,
    fee_value numeric(9,2) NOT NULL,
    fee_start_date date NOT NULL,
    fee_end_date date,
    fee_version numeric NOT NULL,
    fee_changed_by numeric NOT NULL,
    fee_changed_date timestamp without time zone NOT NULL,
    fee_user_name character varying(250),
    is_offsite boolean DEFAULT false
);

CREATE TABLE national_court_houses_master (
    nch_id bigint NOT NULL,
    courthouse_name character varying(100) NOT NULL,
    version_number numeric(38,0) NOT NULL,
    changed_by bigint NOT NULL,
    changed_date timestamp without time zone NOT NULL,
    court_type character varying(10) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    loc_loc_id bigint,
    psa_psa_id bigint,
    court_location_code character varying(10),
    sl_courthouse_name character varying(100),
    norg_id bigint);

CREATE TABLE resolution_codes_master (
    rc_id NUMERIC NOT NULL,
    resolution_code varchar(10) NOT NULL,
    resolution_code_title character varying(500) NOT NULL,
    resolution_code_wording text NOT NULL,
    resolution_legislation text,
    rc_destination_email_address_1 character varying(253),
    rc_destination_email_address_2 character varying(253),
    resolution_code_start_date date NOT NULL,
    resolution_code_end_date date,
    version numeric NOT NULL,
    changed_by numeric NOT NULL,
    changed_date timestamp without time zone NOT NULL,
    user_name character varying(250)
    );  


DROP SEQUENCE IF EXISTS ac_seq;

CREATE SEQUENCE ac_new_seq 
    START WITH 20000
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

DROP SEQUENCE IF EXISTS rc_seq;

CREATE SEQUENCE rc_new_seq 
    START WITH 20000
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

DROP SEQUENCE IF EXISTS nch_seq;

CREATE SEQUENCE nch_new_seq 
    START WITH 20000
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

DROP SEQUENCE IF EXISTS fee_seq;

CREATE SEQUENCE fee_new_seq 
    START WITH 20000
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

DROP SEQUENCE IF EXISTS sa_seq;

CREATE SEQUENCE sa_new_seq 
    START WITH 20000
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 999999999999999999
    CACHE 1;

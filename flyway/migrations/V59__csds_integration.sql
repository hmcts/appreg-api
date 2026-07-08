-- v59__csds_integration.sql

-- Version Control
-- V1.0  	Matthew Harman  06/07/2026	Initial Version
--

CREATE TABLE csds_audit (
	ca_id bigint NOT NULL,
	appreg_table_name varchar(21) NOT NULL,
	appreg_action varchar(12) NOT NULL,
	appreg_key NUMERIC NOT NULL,
	csds_json TEXT
);

CREATE INDEX csds_audit_table_name_i ON csds_audit (appreg_table_name);

DROP SEQUENCE IF EXISTS ca_seq;
CREATE SEQUENCE ca_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 1 CACHE 1;

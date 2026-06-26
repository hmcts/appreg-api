-- V58__arcpoc-1431-create_appreg_user_mapping_table.sql

-- Version Control
-- V1.0  	Matthew Harman  25/06/2026	Initial Version
--

CREATE TABLE appreg_user_mapping (
	legacy_changed_by numeric NOT NULL,
	modern_changed_by TEXT NOT NULL
) ;

ALTER TABLE appreg_user_mapping
ADD CONSTRAINT appreg_user_mapping_pkey PRIMARY KEY (legacy_changed_by);

 
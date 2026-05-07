-- V35_ARCPOC1075_AL_ALE_sequence_mapping_table.sql
--
-- Create the table AL_ALE_SEQUENCE_MAPPING to store the latest sequence number used
-- in application_list_entries for a given application_list
--
-- Version Control
-- V1.0  	 Matthew Harman 23/02/2026	Initial version
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;


CREATE TABLE al_ale_sequence_mapping (
	al_id numeric NOT NULL,
    ale_last_sequence smallint NOT NULL
) ;

ALTER TABLE al_ale_sequence_mapping ADD CONSTRAINT al_ale_sequence_mapping_pk PRIMARY KEY (al_id);

CREATE INDEX al_ale_sequence_mapping_fk_i ON al_ale_sequence_mapping (al_id);

ALTER TABLE al_ale_sequence_mapping ADD CONSTRAINT al_ale_sequence_mapping_al_id_fk FOREIGN KEY (al_id) REFERENCES application_lists(al_id) ON DELETE NO ACTION NOT DEFERRABLE INITIALLY IMMEDIATE;



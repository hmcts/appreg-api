-- V36__ARCPOC1075_AL_ALE_sequence_mapping_populate.sql
--
-- Populate the table AL_ALE_SEQUENCE_MAPPING with the latest sequence number used
-- in application_list_entries for a given application_list
--
-- Version Control
-- V1.0  	 Matthew Harman 23/02/2026	Initial version
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;


INSERT INTO al_ale_sequence_mapping (ale_last_sequence, al_id)
SELECT MAX(sequence_number) AS ale_last_sequence, al_al_id AS al_id
FROM application_list_entries
GROUP BY al_al_id;
-- Populate the al_ale_sequence_mapping table

-- Version Control
-- V1.0  	Matthew Harman  07/04/2026	Initial Version

DELETE FROM al_ale_sequence_mapping;

INSERT INTO al_ale_sequence_mapping (ale_last_sequence, al_id)
SELECT MAX(sequence_number) AS ale_last_sequence, al_al_id AS al_id
FROM application_list_entries
GROUP BY al_al_id;

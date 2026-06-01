-- V42__populate_database_jobs_arcpoc996.sql

-- Version Control
-- V1.0  	Matthew Harman  26/03/2026	Initial Version
--
UPDATE retention_policy
    SET config_key = 'ENABLE_DATA_AUDIT',
        config_value = 'Y',
        config_notes = 'Enable data audit for deletion of application lists'
    WHERE rp_id = 1;

INSERT INTO retention_policy (rp_id, dj_dj_id, retention_policy_name, config_key, config_value, config_notes)
    VALUES (nextval('rp_seq'), 1, 'APPLICATION_LISTS', 'RETENTION_PERIOD_DAYS', '1825', 'Number of days to retain application lists data after list is CLOSED');


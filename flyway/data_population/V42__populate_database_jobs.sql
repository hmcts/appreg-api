-- V42__populate_database_jobs_arcpoc996.sql

-- Version Control
-- V1.0  	Matthew Harman  26/03/2026	Initial Version
--

DELETE FROM database_jobs;

INSERT INTO database_jobs (dj_id, job_name, job_enabled, job_last_ran)
    VALUES (nextval('dj_seq'), 'APPLICATION_LISTS_DATABASE_JOB', 'Y', NULL);

DELETE FROM retention_policy;

INSERT INTO retention_policy (rp_id, dj_dj_id, retention_policy_name, retention_policy_start_date, retention_policy_end_date, retention_policy_metadata)
    VALUES (nextval('rp_seq'), 1, 'APPLICATION_LISTS', '2026-01-01', NULL, NULL);

DELETE FROM retention_policy_configuration;

INSERT INTO retention_policy_configuration (rpc_id, rp_rp_id, config_key, config_value, config_notes)
    VALUES (nextval('rpc_seq'), 1, 'RETENTION_PERIOD_DAYS', '1825', 'Number of days to retain application lists data after list is CLOSED');

INSERT INTO retention_policy_configuration (rpc_id, rp_rp_id, config_key, config_value, config_notes)
    VALUES (nextval('rpc_seq'), 1, 'ENABLE_DATA_AUDIT', 'Y', 'Enable data audit for deletion of application lists');

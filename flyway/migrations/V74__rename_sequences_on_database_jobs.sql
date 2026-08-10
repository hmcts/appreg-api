-- v74__rename_sequences_on_database_jobs.sql 

-- Version Control
-- V1.0  	Matthew Harman  06/08/2026	Initial Version
--


ALTER TABLE database_jobs ALTER COLUMN dj_id DROP DEFAULT;

ALTER TABLE database_job_execution_log ALTER COLUMN djel_id DROP DEFAULT;

DROP SEQUENCE IF EXISTS database_jobs_dj_id_seq;

DROP SEQUENCE IF EXISTS database_job_execution_log_djel_id_seq;

SELECT setval('dj_seq'::regclass, (SELECT MAX(dj_id)::bigint FROM database_jobs));

SELECT setval('djel_seq'::regclass, (SELECT MAX(djel_id)::bigint FROM database_job_execution_log));
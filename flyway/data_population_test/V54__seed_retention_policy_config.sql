-- V54__seed_retention_policy_config.sql
--
-- Populate the inline retention policy config introduced in V53 for the
-- seeded APPLICATION_LISTS_DATABASE_JOB row.

UPDATE retention_policy rp
SET config_key = 'RETENTION_PERIOD_DAYS',
    config_value = '1825',
    config_notes = 'Number of days to retain application lists data after list is CLOSED'
FROM database_jobs dj
WHERE rp.dj_dj_id = dj.dj_id
  AND dj.job_name = 'APPLICATION_LISTS_DATABASE_JOB'
  AND rp.config_key IS NULL;

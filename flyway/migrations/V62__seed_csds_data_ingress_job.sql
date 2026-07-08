INSERT INTO database_jobs (dj_id, job_name, job_enabled, job_last_ran, job_metadata)
VALUES (
    COALESCE((SELECT MAX(dj_id) + 1 FROM database_jobs), 1),
    'CSDS_DATA_INGRESS',
    'Y',
    NULL,
    NULL
)
ON CONFLICT (job_name) DO NOTHING;

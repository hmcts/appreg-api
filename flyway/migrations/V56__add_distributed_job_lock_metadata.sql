ALTER TABLE database_jobs
    ADD COLUMN job_metadata VARCHAR(64);

ALTER TABLE database_jobs
    ADD CONSTRAINT uq_database_jobs_job_name UNIQUE (job_name);

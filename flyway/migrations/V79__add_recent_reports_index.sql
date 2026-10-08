-- Bound recent-report reads by user and completion time without indexing CSV contents.
CREATE INDEX asynch_jobs_recent_reports_idx
    ON asynch_jobs (user_name, last_updated DESC, aj_id DESC)
    WHERE job_state = 'COMPLETED' AND csv_output IS NOT NULL AND csv_output <> '';

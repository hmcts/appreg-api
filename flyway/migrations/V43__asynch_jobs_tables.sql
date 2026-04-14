-- V43__asynch_jobs_tables.sql

-- Version Control
-- V1.0  	Matthew Harman  09/04/2026	Initial Version
--

CREATE TABLE asynch_jobs (
    aj_id SERIAL PRIMARY KEY,
    job_state VARCHAR(9) NOT NULL DEFAULT 'SUBMITTED' CHECK (job_state IN ('SUBMITTED','PENDING','RUNNING','COMPLETED','FAILED')),
    job_type VARCHAR(100) NOT NULL,
    id UUID DEFAULT gen_random_uuid() UNIQUE,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    csv_output TEXT,
    failure_message VARCHAR(400)
);


DROP SEQUENCE IF EXISTS aj_seq;
CREATE SEQUENCE aj_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

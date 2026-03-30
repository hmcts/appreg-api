CREATE TABLE database_jobs (
                             dj_id SERIAL PRIMARY KEY,
                             job_name VARCHAR(255) NOT NULL,
                             job_enabled CHAR(1) DEFAULT 'N' CHECK (job_enabled IN ('Y','N')),
                             job_last_ran TIMESTAMP,
                             rp_rp_id bigint
);

DROP SEQUENCE IF EXISTS dj_seq;
CREATE SEQUENCE dj_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 1;

INSERT INTO database_jobs (dj_id, job_name, job_enabled, rp_rp_id)
VALUES (nextval('dj_seq'), 'APPLICATION_LISTS_DATABASE_JOB', 'Y', 1);

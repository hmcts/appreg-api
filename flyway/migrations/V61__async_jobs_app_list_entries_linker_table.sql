CREATE TABLE async_jobs_app_list_entry (
    aj_ale_id BIGINT PRIMARY KEY,
    aj_id UUID NOT NULL,
    ale_id UUID NOT NULL,
    FOREIGN KEY (aj_id) REFERENCES asynch_jobs(id),
    FOREIGN KEY (ale_id) REFERENCES application_list_entries(id)
);

DROP SEQUENCE IF EXISTS aj_ale_seq;
CREATE SEQUENCE aj_ale_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

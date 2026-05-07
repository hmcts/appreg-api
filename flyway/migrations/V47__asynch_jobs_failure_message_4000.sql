-- V47__asynch_jobs_failure_message_4000.sql

-- Version Control
-- V1.0  	Matthew Harman  21/04/2026	Initial Version
--

alter table asynch_jobs
  alter column failure_message type varchar(4000);


-- V46__add_async_user_column.sql

-- Version Control
-- V1.0  	Matthew Harman  09/04/2026	Initial Version
--

ALTER TABLE asynch_jobs
  ADD COLUMN user_name varchar(250) NOT NULL;

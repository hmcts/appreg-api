-- Create the scheduler job

-- Version Control
-- V1.0  	Matthew Harman  07/04/2026	Initial Version

select postgres.cron.schedule_in_database(
	'APPLICATION_LISTS_DATABASE_JOB',
	'0 16 * * *',
	'CALL appreg.delete_expired_application_lists()',
	'appreg-db');
-- V4__metadata_exclusion_for_CSDS.sql

-- Version Control
-- V1.0  	Matthew Harman  19/10/2026	Initial Version

INSERT INTO metadata_exclusion (oracle_schema, table_name, column_name, issue, reason, created_at)
VALUES 
('APPREGISTER', 'APPLICATION_CODES', 'CSDS_ID', 'missing_in_oracle_snapshot', 'CSDS_ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'NATIONAL_COURT_HOUSES', 'CSDS_ID', 'missing_in_oracle_snapshot', 'CSDS_ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'RESOLUTION_CODES', 'CSDS_ID', 'missing_in_oracle_snapshot', 'CSDS_ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'FEE', 'CSDS_ID', 'missing_in_oracle_snapshot', 'CSDS_ID is a new PostgreSQL-only column added by design', NOW()),
('APPREGISTER', 'STANDARD_APPLICANTS', 'CSDS_ID', 'missing_in_oracle_snapshot', 'CSDS_ID is a new PostgreSQL-only column added by design', NOW())
;

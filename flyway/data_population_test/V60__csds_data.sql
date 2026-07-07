-- V60__csds_data.sql

-- Version Control
-- V1.0  	Matthew Harman  06/07/2026	Initial Version
--

INSERT INTO configuration_parameters (cp_id, parameter_name, parameter_type, parameter_value, parameter_purpose)
    VALUES (nextval('cp_seq'), 'AUDIT_CSDS', 'STRING', 'DEBUG', 'CSDS audit level DEBUG|ERROR|NONE');


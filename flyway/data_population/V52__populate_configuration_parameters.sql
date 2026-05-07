-- V52__populate_configuration_parameters.sql

-- Version Control
-- V1.0  	Matthew Harman  29/04/2026	Initial Version
--

DELETE FROM configuration_parameters;

INSERT INTO configuration_parameters (cp_id, parameter_name, parameter_type, parameter_value, parameter_purpose)
    VALUES (nextval('cp_seq'), 'MAX_APPS_FEES', 'NUMBER', 100, 'Maximum application fees allowed per report');


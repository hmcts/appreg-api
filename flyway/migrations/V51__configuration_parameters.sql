-- V51__configuration_parameters.sql

-- Version Control
-- V1.0  	Matthew Harman  29/04/2026	Initial Version
--

CREATE TABLE configuration_parameters (
    cp_id SERIAL PRIMARY KEY,
    parameter_name VARCHAR(50) NOT NULL,
    parameter_type VARCHAR(20) NOT NULL,
    parameter_value VARCHAR(50) NOT NULL,
    parameter_purpose VARCHAR(200) NOT NULL
);


DROP SEQUENCE IF EXISTS cp_seq;
CREATE SEQUENCE cp_seq INCREMENT 1 MINVALUE 1 START 1 CACHE 1;

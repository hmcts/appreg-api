-- V48__name_address_new_fields.sql

-- Version Control
-- V1.0  	Matthew Harman  28/04/2026	Initial Version
--

-- Add new fields to name and address tables
-- first_name
-- middle_name
-- last_name
alter table name_address
  add column first_name varchar(100),
  add column middle_name varchar(100),
  add column last_name varchar(100);


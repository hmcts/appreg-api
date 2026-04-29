-- V49__pop_new_name_address_fields.sql

-- Version Control
-- V1.0  	Matthew Harman  28/04/2026	Initial Version
--

update name_address
set first_name = forename_1;

update name_address
set middle_name = rtrim(ltrim(replace(concat(forename_2,' ',forename_3),'  ',' ')));

update name_address
set last_name = surname;
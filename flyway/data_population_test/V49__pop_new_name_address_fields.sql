-- V49__pop_new_name_address_fields.sql

-- Version Control
-- V1.0  	Matthew Harman  28/04/2026	Initial Version
-- V1.1.  Colin Bradshaw. 28/04/2026  Fix up test data
--

-- Normalise legacy test fixtures before updating rows protected by
-- name_address_name_or_person_chk.
update name_address
set name = null
where na_id in (1, 3);

update name_address
set title = null,
    forename_1 = null,
    forename_2 = null,
    forename_3 = null,
    surname = null
where na_id = 2;

update name_address
set first_name = forename_1;

update name_address
set middle_name = rtrim(ltrim(replace(concat(forename_2,' ',forename_3),'  ',' ')));

update name_address
set last_name = surname;

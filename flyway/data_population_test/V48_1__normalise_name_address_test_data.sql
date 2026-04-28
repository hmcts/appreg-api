-- V48_1__normalise_name_address_test_data.sql

-- Version Control
-- V1.0  	Codex  28/04/2026	Normalise legacy test fixtures before name backfill
--

-- Person fixtures must not also carry an organisation name.
update name_address
set name = null
where na_id in (1, 3);

-- This fixture is used as a respondent organisation in integration tests.
update name_address
set title = null,
    forename_1 = null,
    forename_2 = null,
    forename_3 = null,
    surname = null
where na_id = 2;

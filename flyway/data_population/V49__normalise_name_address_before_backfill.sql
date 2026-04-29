-- V49__normalise_name_address_before_backfill.sql

-- Version Control
-- V1.0  	Codex  28/04/2026	Normalise legacy data before name backfill
--

-- Organisation-style rows must not carry person fields.
update name_address
set title = null,
    forename_1 = null,
    forename_2 = null,
    forename_3 = null,
    surname = null
where forename_1 is null;

-- Person-style rows must not also carry an organisation name.
update name_address
set name = null
where forename_1 is not null;

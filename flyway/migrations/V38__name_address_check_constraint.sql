-- V38__name_address_check_constraint.sql

-- Version Control
-- V1.0  	Matthew Harman  04/03/2026	Initial Version
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

-- Add the CHECK constraint (drop existing constraint if present first)
ALTER TABLE IF EXISTS name_address
    DROP CONSTRAINT IF EXISTS name_address_name_or_person_chk;


ALTER TABLE name_address
ADD CONSTRAINT name_address_name_or_person_chk
CHECK (
    (
        -- Scenario 1: organisation-style
        NULLIF(BTRIM(name), '') IS NOT NULL
        AND NULLIF(BTRIM(title), '')      IS NULL
        AND NULLIF(BTRIM(forename_1), '') IS NULL
        AND NULLIF(BTRIM(forename_2), '') IS NULL
        AND NULLIF(BTRIM(forename_3), '') IS NULL
        AND NULLIF(BTRIM(surname), '')    IS NULL
    )
    OR
    (
        -- Scenario 2: person-style
        NULLIF(BTRIM(name), '') IS NULL
        AND NULLIF(BTRIM(title), '')      IS NOT NULL
        AND NULLIF(BTRIM(forename_1), '') IS NOT NULL
        AND NULLIF(BTRIM(surname), '')    IS NOT NULL
    )
    
    -- Global dependency: forename_2 allowed only if forename_1 populated
    AND (
        NULLIF(BTRIM(forename_2), '') IS NULL
        OR NULLIF(BTRIM(forename_1), '') IS NOT NULL
    )
    
    -- Global dependency: forename_3 allowed only if forename_1 populated
    AND (
        NULLIF(BTRIM(forename_3), '') IS NULL
        OR NULLIF(BTRIM(forename_1), '') IS NOT NULL
    )
)
NOT VALID;

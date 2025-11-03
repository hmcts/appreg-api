-- V7__add_choa_courts.sql

-- Mark a couple of seeded courts as CHOA so findActiveCourt will return rows
UPDATE national_court_houses
SET court_type = 'CHOA'
WHERE nch_id IN (3, 6);

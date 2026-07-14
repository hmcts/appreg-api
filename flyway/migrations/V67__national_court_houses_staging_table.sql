CREATE TABLE national_court_houses_staging (
    nch_id BIGINT NOT NULL,
    courthouse_name varchar(100) NOT NULL,
    version_number NUMERIC(38) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_date timestamp NOT NULL,
    court_type varchar(10) NOT NULL,
    start_date timestamp NOT NULL,
    end_date timestamp,
    loc_loc_id BIGINT,
    psa_psa_id BIGINT,
    court_location_code varchar(10),
    sl_courthouse_name varchar(100),
    norg_id BIGINT
);

CREATE INDEX nchs_court_location_code_idx
    ON national_court_houses_staging (court_location_code);
ALTER TABLE national_court_houses_staging
    ADD CONSTRAINT national_court_houses_staging_pk PRIMARY KEY (nch_id);

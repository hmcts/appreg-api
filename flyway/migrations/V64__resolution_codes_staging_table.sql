CREATE TABLE resolution_codes_staging (
    rc_id NUMERIC NOT NULL,
    resolution_code varchar(10) NOT NULL,
    resolution_code_title varchar(500) NOT NULL,
    resolution_code_wording text NOT NULL,
    resolution_legislation text,
    rc_destination_email_address_1 varchar(253),
    rc_destination_email_address_2 varchar(253),
    resolution_code_start_date timestamp NOT NULL,
    resolution_code_end_date timestamp,
    version NUMERIC NOT NULL,
    changed_by NUMERIC NOT NULL,
    changed_date timestamp NOT NULL,
    user_name varchar(250)
);

CREATE INDEX rcs_resolution_code_idx ON resolution_codes_staging (resolution_code);
ALTER TABLE resolution_codes_staging ADD CONSTRAINT resolution_codes_staging_pk PRIMARY KEY (rc_id);

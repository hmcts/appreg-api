CREATE TABLE application_codes_staging (
    ac_id NUMERIC NOT NULL,
    application_code varchar(10) NOT NULL,
    application_code_title varchar(500) NOT NULL,
    application_code_wording text NOT NULL,
    application_legislation text,
    fee_due char(1) NOT NULL,
    application_code_respondent char(1) NOT NULL,
    ac_destination_email_address_1 varchar(253),
    ac_destination_email_address_2 varchar(253),
    application_code_start_date timestamp NOT NULL,
    application_code_end_date timestamp,
    bulk_respondent_allowed char(1) NOT NULL,
    version NUMERIC NOT NULL,
    changed_by NUMERIC NOT NULL,
    changed_date timestamp NOT NULL,
    user_name varchar(250),
    ac_fee_reference varchar(12)
);

CREATE INDEX acs_application_code_idx ON application_codes_staging (application_code);
CREATE INDEX acs_fee_reference_idx ON application_codes_staging (ac_fee_reference);
ALTER TABLE application_codes_staging ADD CONSTRAINT application_codes_staging_pk PRIMARY KEY (ac_id);

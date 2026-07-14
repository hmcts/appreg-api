CREATE TABLE fee_staging (
    fee_id NUMERIC NOT NULL,
    fee_reference varchar(12) NOT NULL,
    fee_description varchar(250) NOT NULL,
    fee_value numeric NOT NULL,
    fee_start_date timestamp NOT NULL,
    fee_end_date timestamp,
    fee_version NUMERIC NOT NULL,
    fee_changed_by NUMERIC NOT NULL,
    fee_changed_date timestamp NOT NULL,
    fee_user_name varchar(250) NOT NULL,
    is_offsite boolean DEFAULT false
);

CREATE INDEX fee_staging_reference_idx ON fee_staging (fee_reference);
ALTER TABLE fee_staging ADD CONSTRAINT fee_staging_pk PRIMARY KEY (fee_id);

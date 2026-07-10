CREATE TABLE standard_applicants_staging (
    sa_id NUMERIC NOT NULL,
    standard_applicant_code varchar(10) NOT NULL,
    standard_applicant_start_date date NOT NULL,
    standard_applicant_end_date date,
    version NUMERIC NOT NULL,
    changed_by NUMERIC NOT NULL,
    changed_date timestamp NOT NULL,
    user_name varchar(250),
    name varchar(100),
    title varchar(100),
    forename_1 varchar(100),
    forename_2 varchar(100),
    forename_3 varchar(100),
    surname varchar(100),
    address_l1 varchar(35) NOT NULL,
    address_l2 varchar(35),
    address_l3 varchar(35),
    address_l4 varchar(35),
    address_l5 varchar(35),
    postcode varchar(8),
    email_address varchar(253),
    telephone_number varchar(20),
    mobile_number varchar(20)
);

CREATE INDEX sas_standard_applicant_code_idx
    ON standard_applicants_staging (standard_applicant_code);
ALTER TABLE standard_applicants_staging ADD CONSTRAINT standard_applicants_staging_pk PRIMARY KEY (sa_id);

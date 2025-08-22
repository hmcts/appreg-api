insert into appreg.application_codes
(select ac_id, 
replace(replace(replace(replace(replace(application_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(application_code_title,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(application_code_wording,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(application_legislation,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(fee_due,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(application_code_respondent,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(ac_destination_email_address_1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(ac_destination_email_address_2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
application_code_start_date::timestamp,
application_code_end_date::timestamp,
replace(replace(replace(replace(replace(bulk_respondent_allowed,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(ac_fee_reference,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.application_codes_temp);

insert into appreg.application_lists
(select al_id,
replace(replace(replace(replace(replace(application_list_status,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
application_list_date::timestamp,
application_list_time::timestamp,
replace(replace(replace(replace(replace(courthouse_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(other_courthouse,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(list_description,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(courthouse_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
duration_hour,
duration_minute,
cja_cja_id
from appreg.application_lists_temp);

insert into appreg.application_list_entries
(select ale_id,
al_al_id,
sa_sa_id,
ac_ac_id,
a_na_id,
r_na_id,
number_of_bulk_respondents,
replace(replace(replace(replace(replace(application_list_entry_wording,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(case_reference,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(account_number,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(entry_rescheduled,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(notes,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(bulk_upload,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
sequence_number,
replace(replace(replace(replace(replace(tcep_status,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(message_uuid,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(retry_count,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
lodgement_date
from appreg.application_list_entries_temp);

-- APPLICATION_REGISTER
WITH unescaped AS (
  SELECT
    ar_id,
    al_al_id,
    changed_by,
    (changed_date_utc)::timestamptz AS changed_date,
    NULLIF(user_name,'') AS user_name,
    piece_no,
    -- unescape in safe order (delimiter/control chars first, backslash last)
    REPLACE(
      REPLACE(
        REPLACE(
          REPLACE(
            REPLACE(piece_escaped, '\p', '|'),
          '\t', E'\t'),
        '\r', E'\r'),
      '\n', E'\n'),
    '\\', '\'
    ) AS piece
  FROM appreg.application_register_temp
),
reconstructed AS (
  SELECT
    ar_id, al_al_id, changed_by, changed_date, user_name,
    NULLIF(string_agg(piece, '' ORDER BY piece_no), '') AS text_full
  FROM unescaped
  GROUP BY ar_id, al_al_id, changed_by, changed_date, user_name
)
INSERT INTO appreg.application_register
  (ar_id, al_al_id, "text", changed_by, changed_date, user_name)
SELECT
  ar_id,
  al_al_id,
  text_full,
  changed_by,
  changed_date,
  CASE WHEN user_name IS NULL OR user_name = '' THEN NULL ELSE left(user_name, 250) END
FROM reconstructed;

insert into appreg.app_list_entry_fee_id
(select ale_ale_id,
fee_fee_id,
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.app_list_entry_fee_id_temp);

insert into appreg.app_list_entry_fee_status
(select alefs_id,
alefs_ale_id,
replace(replace(replace(replace(replace(alefs_payment_reference,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(alefs_fee_status,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
alefs_fee_status_date,
alefs_version,
alefs_changed_by,
alefs_changed_date,
replace(replace(replace(replace(replace(alefs_user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
alefs_status_creation_date
from appreg.app_list_entry_fee_status_temp);

insert into appreg.app_list_entry_official
(select aleo_id,
ale_ale_id,
replace(replace(replace(replace(replace(title,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(surname,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(official_type,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.app_list_entry_official_temp);

insert into appreg.app_list_entry_resolutions
(select aler_id,
rc_rc_id,
ale_ale_id,
replace(replace(replace(replace(replace(al_entry_resolution_wording,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(al_entry_resolution_officer,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.app_list_entry_resolutions_temp);

insert into appreg.criminal_justice_area
(select cja_id,
replace(replace(replace(replace(replace(cja_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(cja_description,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.criminal_justice_area_temp);

insert into appreg.fee
(select fee_id,
replace(replace(replace(replace(replace(fee_reference,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(fee_description,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
fee_value,
fee_start_date,
fee_end_date,
fee_version,
fee_changed_by,
fee_changed_date,
replace(replace(replace(replace(replace(fee_user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.fee_temp);

insert into appreg.name_address
(select na_id,
replace(replace(replace(replace(replace(code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(title,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_3,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(surname,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l3,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l4,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l5,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(postcode,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(email_address,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(telephone_number,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(mobile_number,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
date_of_birth,
replace(replace(replace(replace(replace(dms_id,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.name_address_temp);

insert into appreg.resolution_codes
(select rc_id,
replace(replace(replace(replace(replace(resolution_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(resolution_code_title,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(resolution_code_wording,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(resolution_legislation,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(rc_destination_email_address_1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(rc_destination_email_address_2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
resolution_code_start_date,
resolution_code_end_date,
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.resolution_codes_temp);

insert into appreg.standard_applicants
(select sa_id,
replace(replace(replace(replace(replace(standard_applicant_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
standard_applicant_start_date,
standard_applicant_end_date,
version,
changed_by,
changed_date,
replace(replace(replace(replace(replace(user_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(title,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(forename_3,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(surname,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l3,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l4,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(address_l5,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(postcode,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(email_address,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(telephone_number,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(mobile_number,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.standard_applicants_temp);

insert into appreg.national_court_houses
(select nch_id,
replace(replace(replace(replace(replace(courthouse_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version_number,
changed_by,
changed_date,
replace(replace(replace(replace(replace(court_type,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
loc_loc_id,
psa_psa_id,
replace(replace(replace(replace(replace(court_location_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(sl_courthouse_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
norg_id
from appreg.national_court_houses_temp);

insert into appreg.link_addresses
(select la_id,
replace(replace(replace(replace(replace(no_fixed_abode,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(la_type,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
version_number,
changed_by,
changed_date,
adr_adr_id,
bu_bu_id,
er_er_id,
loc_loc_id,
replace(replace(replace(replace(replace(head_office_indicator,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t')
from appreg.link_addresses_temp);

insert into appreg.addresses
(select adr_id,
replace(replace(replace(replace(replace(line1,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(line2,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(line3,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(line4,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(line5,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(postcode,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
version_number,
changed_by,
changed_date,
mcc_mcc_id
from appreg.addresses_temp);

insert into appreg.link_communication_media
(select lcm_id,
replace(replace(replace(replace(replace(lcm_type,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
version_number,
changed_by,
changed_date,
comm_comm_id,
loc_loc_id,
er_er_id,
bu_bu_id
from appreg.link_communication_media_temp);

insert into appreg.communication_media
(select comm_id,
replace(replace(replace(replace(replace(detail,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
version_number,
changed_by,
changed_date
from appreg.communication_media_temp);

insert into appreg.petty_sessional_areas
(select psa_id,
replace(replace(replace(replace(replace(psa_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(short_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
version_number,
changed_by,
changed_date,
cma_cma_id,
replace(replace(replace(replace(replace(psa_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
start_date,
end_date,
replace(replace(replace(replace(replace(jc_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
replace(replace(replace(replace(replace(court_type,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
crime_cases_loc_id,
fine_accounts_loc_id,
maintenance_enforcement_loc_id,
family_cases_loc_id,
replace(replace(replace(replace(replace(court_location_code,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
central_finance_loc_id,
replace(replace(replace(replace(replace(sl_psa_name,'\\','\'),'\p','|'),'\r',E'\r'),'\n',E'\n'),'\t',E'\t'),
norg_id
from appreg.petty_sessional_areas_temp);

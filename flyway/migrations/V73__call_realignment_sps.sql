-- v73__call_realignment_sps.sql 

-- Version Control
-- V1.0  	Matthew Harman  22/07/2026	Initial Version
--

call ${flyway:defaultSchema}.realign_application_codes();

call ${flyway:defaultSchema}.realign_resolution_codes();

call ${flyway:defaultSchema}.realign_national_court_houses();

call ${flyway:defaultSchema}.realign_fees();

call ${flyway:defaultSchema}.realign_standard_applicants();

update ${flyway:defaultSchema}.fee set is_offsite = true where fee_reference = 'CO1.1';

update ${flyway:defaultSchema}.fee set is_offsite = false where fee_reference <> 'CO1.1';


-- Add EF application codes and associated application_lists, etc..

-- Version Control
-- V1.0  	Matthew Harman      04/02/2026	Initial version
--

-- Add new records
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
        (680, 'EF99001','Collection Order - Financial Penalty Account','Application for a collection order to enforce the remaining balance on a financial penalty account of {TEXT|account balance|10}','Section 5 Courts Act 2003','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (681, 'EF99002','Collection Order - Confiscation Account','Application for a collection order to enforce the remaining balance on a confiscation order account of {TEXT|account balance|10}','Section 5 Courts Act 2003','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (682, 'EF99003','Collection Order - Crown Court  Account','Application for a collection order to enforce the remaining balance on a Crown Court financial penalty account of {TEXT|account balance|10} with payment terms as set by the Crown Court.','Section 5 Courts Act 2003','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (683, 'EF99004','TFO England and Wales','Application to transfer enforcement of the financial penalty to {TEXT| LJA Name|120}','Section 81 of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (684, 'EF99005','TFO Scotland','Application to transfer enforcement of the financial penalty to Scottish court: {TEXT| Scottish Court Name|120}','Section 82 of the Magistrates Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (685, 'EF99006','TFO Northern Ireland','Application to transfer enforcement of the financial penalty to Northern Ireland court: {TEXT| N Irish Court Name|120}','Section 82 of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (686, 'EF99007','Warrant of Control','Application for a warrant of control to collect the outstanding balance of {TEXT|Account balance|10}','Section 76(1) of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (687, 'EF99008','Arrest warrant (fine account) - no bail','Application for the issue of an arrest warrant (without bail) to attend court for means enquiry to enforce the remaining balance of {TEXT| Account balance|10}','Section 83 of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (688, 'EF99009','Arrest warrant (fine account) with bail','Application for the issue of an arrest warrant (with bail) to attend court for means enquiry to enforce the remaining balance of {TEXT| Account balance|10}','Section 83 of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (721, 'EF99011','Collection Order - Registered Fixed Penalty Account','Application for a collection order to enforce the oustanding sum on a registered fixed penalty account of {TEXT|account balance|10} with payment terms of 14 days.','Section 5 Courts Act 2003','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1082, 'EF99012','Warrant to authorise use of reasonable force to execute a warrant of control','Application for a warrant to authorise the use of reasonable force to execute a warrant of control. Balance: {CUR|l_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last Payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Paragraph 20 Schedule 12 Tribunals, Courts and Enforcement Act 2007','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1083, 'EF99013','Warrant to authorise use of reasonable force to take control of goods on a highway','Application for a warrant to authorise the use of reasonable force to  take control of goods on a highway. Balance {CUR_1_A/C Balance|10} Date imposed: {DT|2_Date_imposed|10} Last payment: {DT|3_Date_last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Paragraph 31 Schedule 12 Tribunals, Courts and Enforcement Act 2007','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1084, 'EF99014','Order to extend time limit to take control of goods','Application for an order to extend the period of time for taking control of goods by 12 months. Balance {CUR|1_A/C Balance|10} Date imposed {DT|2_Data imposed|10} Last payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Regulation 9(3) Taking Control of Goods Regulations 2013','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1085, 'EF99015','Order to shorten period of notice prior to taking control of goods','Application for an order to specifying less than 7 dyas as the period of time for giving notice of enforcement prior to taking control of goods. Balance: {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Regulation 6(3) Taking Control of Goods Regulations 2013','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1086, 'EF99016','Authority to take control of goods outside specified hours','Application for an order to take control of goods outside the hours specified in Regulations, namely {TXT|1_Hours requested|250}','Regulation 13(2)(a) Taking Control of Goods Regulations 2013','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1087, 'EF99017','Authority to enter or remain on premises outside specified h','Application for authority to enter re-enter or remain on premises other than during the hours sepcified in Regulations, namely {TXT|1_Hours requested|250}','Regulation 22(5) Taking Control of Goods Regulations 2013','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1088, 'EF99018','Warrant to enter premises to search for and take control of goods','Application for a warrant to enter premises at {TXT|1_Specified_address|150} to search for and take control of goods','Paragraph 15 Sch 12 Tribunals, Courts and Enforcement Act 2007','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1089, 'EF99019','Order for the same of goods to be other than by pubic auction','Application for an order that goods may be sold other than by public auction, namely {TXT|1_Method of sale|150}, on the grounds that {TXT|1_Specify reasons|200}','Paragraph 41 Sch 12 Tribunals, Courts and Enforcement Act 2007','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1090, 'EF99020','Order for the disposal of uncollected controlled goods','Application for an order to determine the method of disposal of uncollected controlled goods','Regulation 47(5) Taking Control of Goods Regulations 2013','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1062, 'EF99021','Summons for enforcement of a fine account against a youth','Application for a summons for a youth debtor to attend court for means enquiry. Balance {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Section 83 Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1282, 'EF99022','Summons for enforcement of a fine account corporate or organisational debtor','Application for a summons for a corporate or organisation debtor to attend court for means enquiry. Balance: {CUR|1_A/C Balance|10} Date imposed: {DT|2_Date imposed|10} Last payment: {DT|3_Date last payment|10} Last enforcement: {TXT|4_Last enforcement|10}','Section 83 Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL),
        (1842, 'EF99023','Warrant of Control (Company)','Application for a warranty of control in relation to a company to collect the outstanding balance of {CUR|1_A/C Balance|10}','Section 76(1) of the Magistrates Courts Act 1980','N','Y',NULL,NULL,DATE '2016-01-01',NULL,'N', 1, 234, DATE '2018-09-18', 'admin', NULL)
;

-- Reset the sequence for application_codes.ac_id;
SELECT setval('ac_seq'::regclass, (SELECT MAX(ac_id)::bigint FROM application_codes));


-- Create APPLICATION_LISTS, so that we can create application list entries that 
-- hang off the application codes just created
INSERT INTO application_lists (
        al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse,
        list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id
)
VALUES
        (70000,'OPEN', '2024-04-25', '10:00:00', 'RCJ101', null, 'Test List for application code EF99001', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99001', 6, 3, NULL),
        (70001,'OPEN', '2024-04-25', '10:00:00', 'RCJ111', null, 'Test List for application code EF99002', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99002', 6, 3, NULL),
        (70002,'OPEN', '2024-04-25', '11:00:00', 'RCJ121', null, 'Test List for application code EF99003', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99003', 6, 3, NULL),
        (70003,'OPEN', '2024-04-25', '10:00:00', 'RCJ131', null, 'Test List for application code EF99004', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99004', 6, 3, NULL),
        (70004,'OPEN', '2024-04-25', '11:00:00', 'RCJ141', null, 'Test List for application code EF99005', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99005', 6, 3, NULL),
        (70005,'OPEN', '2024-04-25', '10:00:00', 'RCJ151', null, 'Test List for application code EF99006', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99006', 6, 3, NULL),
        (70006,'OPEN', '2024-04-25', '11:00:00', 'RCJ161', null, 'Test List for application code EF99007', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99007', 6, 3, NULL),
        (70007,'OPEN', '2024-04-25', '10:00:00', 'RCJ171', null, 'Test List for application code EF99008', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99008', 6, 3, NULL),
        (70008,'OPEN', '2024-04-25', '11:00:00', 'RCJ181', null, 'Test List for application code EF99009', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99009', 6, 3, NULL),
        (70009,'OPEN', '2024-04-25', '10:00:00', 'RCJ191', null, 'Test List for application code EF99010', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99011', 6, 3, NULL),
        (70010,'OPEN', '2024-04-25', '11:00:00', 'RCJ201', null, 'Test List for application code EF99011', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99012', 6, 3, NULL),
        (70011,'OPEN', '2024-04-25', '10:00:00', 'RCJ211', null, 'Test List for application code EF99012', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99013', 6, 3, NULL),
        (70012,'OPEN', '2024-04-25', '11:00:00', 'RCJ221', null, 'Test List for application code EF99013', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99014', 6, 3, NULL),
        (70013,'OPEN', '2024-04-25', '10:00:00', 'RCJ231', null, 'Test List for application code EF99014', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99015', 6, 3, NULL),
        (70014,'OPEN', '2024-04-25', '11:00:00', 'RCJ241', null, 'Test List for application code EF99015', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99016', 6, 3, NULL),
        (70015,'OPEN', '2024-04-25', '10:00:00', 'RCJ251', null, 'Test List for application code EF99016', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99017', 6, 3, NULL),
        (70016,'OPEN', '2024-04-25', '11:00:00', 'RCJ261', null, 'Test List for application code EF99017', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99018', 6, 3, NULL),
        (70017,'OPEN', '2024-04-25', '10:00:00', 'RCJ271', null, 'Test List for application code EF99018', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99019', 6, 3, NULL),
        (70018,'OPEN', '2024-04-25', '11:00:00', 'RCJ281', null, 'Test List for application code EF99019', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99020', 6, 3, NULL),
        (70019,'OPEN', '2024-04-25', '10:00:00', 'RCJ291', null, 'Test List for application code EF99020', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99021', 6, 3, NULL),
        (70020,'OPEN', '2024-04-25', '11:00:00', 'RCJ301', null, 'Test List for application code EF99021', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99022', 6, 3, NULL),
        (70021,'OPEN', '2024-04-25', '10:00:00', 'RCJ311', null, 'Test List for application code EF99022', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'RCJ FE99023', 6, 3, NULL)
;

-- Reset the sequence for application_lists.al_id;
SELECT setval('al_seq'::regclass, (SELECT MAX(al_id)::bigint FROM application_lists));

-- Create many many more respondents in the NAME_ADDRESS table, as we can only use 1 respondent per
-- application list entry.
INSERT INTO name_address (na_id, code, name, title, forename_1, forename_2, forename_3, surname, address_l1, address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number, version, changed_by, changed_date, user_name, date_of_birth, dms_id)
VALUES
    (20000, 'RE', NULL, 'Mr', 'Oliver', NULL, NULL, 'King', '28 Birch Way', NULL, NULL, NULL, NULL, 'B1 1BA', 'oliver.king@example.com', '01234567917', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20001, 'RE', NULL, 'Ms', 'Poppy', NULL, NULL, 'Wright', '29 Oak Avenue', NULL, NULL, NULL, NULL, 'C2 2CB', 'poppy.wright@example.com', '01234567918', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20002, 'RE', NULL, 'Mr', 'Jacob', NULL, NULL, 'Lopez', '30 Pine Road', NULL, NULL, NULL, NULL, 'D3 3DC', 'jacob.lopez@example.com', '01234567919', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20003, 'RE', NULL, 'Mrs', 'Lily', NULL, NULL, 'Hill', '31 Cedar Street', NULL, NULL, NULL, NULL, 'E4 4ED', 'lily.hill@example.com', '01234567920', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20004, 'RE', NULL, 'Mr', 'Noah', NULL, NULL, 'Scott', '32 Elm Lane', NULL, NULL, NULL, NULL, 'F5 5FE', 'noah.scott@example.com', '01234567921', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20005, 'RE', NULL, 'Ms', 'Emma', NULL, NULL, 'Green', '33 Ash Road', NULL, NULL, NULL, NULL, 'G6 6GF', 'emma.green@example.com', '01234567922', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20006, 'RE', NULL, 'Mr', 'Liam', NULL, NULL, 'Adams', '34 Willow Street', NULL, NULL, NULL, NULL, 'H7 7HG', 'liam.adams@example.com', '01234567923', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20007, 'RE', NULL, 'Mrs', 'Zoe', NULL, NULL, 'Nelson', '35 Maple Avenue', NULL, NULL, NULL, NULL, 'I8 8IH', 'zoe.nelson@example.com', '01234567924', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20008, 'RE', NULL, 'Mr', 'Harry', NULL, NULL, 'Carter', '36 Oak Lane', NULL, NULL, NULL, NULL, 'J9 9JI', 'harry.carter@example.com', '01234567925', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20009, 'RE', NULL, 'Ms', 'Ella', NULL, NULL, 'Mitchell', '37 Pine Avenue', NULL, NULL, NULL, NULL, 'K0 0KJ', 'ella.mitchell@example.com', '01234567926', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20010, 'RE', NULL, 'Mr', 'William', NULL, NULL, 'Harris', '38 Birch Road', NULL, NULL, NULL, NULL, 'A8 8AA', 'william.harris@example.com', '01234567927', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20011, 'RE', NULL, 'Ms', 'Sophie', NULL, NULL, 'Young', '39 Oak Avenue', NULL, NULL, NULL, NULL, 'B9 9BB', 'sophie.young@example.com', '01234567928', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20012, 'RE', NULL, 'Mr', 'James', NULL, NULL, 'Hall', '40 Pine Lane', NULL, NULL, NULL, NULL, 'C0 0CC', 'james.hall@example.com', '01234567929', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20013, 'RE', NULL, 'Mrs', 'Chloe', NULL, NULL, 'Allen', '41 Cedar Street', NULL, NULL, NULL, NULL, 'D1 1DD', 'chloe.allen@example.com', '01234567930', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20014, 'RE', NULL, 'Mr', 'Daniel', NULL, NULL, 'King', '42 Elm Avenue', NULL, NULL, NULL, NULL, 'E2 2EE', 'daniel.king@example.com', '01234567931', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20015, 'RE', NULL, 'Ms', 'Ella', NULL, NULL, 'Wright', '43 Ash Road', NULL, NULL, NULL, NULL, 'F3 3FF', 'ella.wright@example.com', '01234567932', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20016, 'RE', NULL, 'Mr', 'Lucas', NULL, NULL, 'Scott', '44 Willow Lane', NULL, NULL, NULL, NULL, 'G4 4GG', 'lucas.scott@example.com', '01234567933', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20017, 'RE', NULL, 'Mrs', 'Mia', NULL, NULL, 'Adams', '45 Maple Avenue', NULL, NULL, NULL, NULL, 'H5 5HH', 'mia.adams@example.com', '01234567934', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20018, 'RE', NULL, 'Mr', 'Noah', NULL, NULL, 'Nelson', '46 Birch Way', NULL, NULL, NULL, NULL, 'I6 6II', 'noah.nelson@example.com', '01234567935', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20019, 'RE', NULL, 'Ms', 'Zoe', NULL, NULL, 'Mitchell', '47 Oak Street', NULL, NULL, NULL, NULL, 'J7 7JJ', 'zoe.mitchell@example.com', '01234567936', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20020, 'RE', NULL, 'Mr', 'Oliver', NULL, NULL, 'Harris', '48 Pine Road', NULL, NULL, NULL, NULL, 'K8 8KK', 'oliver.harris@example.com', '01234567937', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20021, 'RE', NULL, 'Ms', 'Emily', NULL, NULL, 'Clark', '49 Cedar Avenue', NULL, NULL, NULL, NULL, 'L9 9LL', 'emily.clark@example.com', '01234567938', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20022, 'RE', NULL, 'Mr', 'James', NULL, NULL, 'Robinson', '50 Elm Lane', NULL, NULL, NULL, NULL, 'M0 0MM', 'james.robinson@example.com', '01234567939', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20023, 'RE', NULL, 'Mrs', 'Charlotte', NULL, NULL, 'Lewis', '51 Ash Road', NULL, NULL, NULL, NULL, 'N1 1NN', 'charlotte.lewis@example.com', '01234567940', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20024, 'RE', NULL, 'Mr', 'Henry', NULL, NULL, 'Walker', '52 Willow Way', NULL, NULL, NULL, NULL, 'O2 2OO', 'henry.walker@example.com', '01234567941', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20025, 'RE', NULL, 'Ms', 'Grace', NULL, NULL, 'Hall', '53 Maple Avenue', NULL, NULL, NULL, NULL, 'P3 3PP', 'grace.hall@example.com', '01234567942', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20026, 'RE', NULL, 'Mr', 'Liam', NULL, NULL, 'Young', '54 Birch Road', NULL, NULL, NULL, NULL, 'Q4 4QQ', 'liam.young@example.com', '01234567943', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20027, 'RE', NULL, 'Mrs', 'Sophie', NULL, NULL, 'King', '55 Oak Avenue', NULL, NULL, NULL, NULL, 'R5 5RR', 'sophie.king@example.com', '01234567944', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20028, 'RE', NULL, 'Mr', 'Oliver', NULL, NULL, 'Scott', '56 Pine Lane', NULL, NULL, NULL, NULL, 'S6 6SS', 'oliver.scott@example.com', '01234567945', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20029, 'RE', NULL, 'Ms', 'Emily', NULL, NULL, 'Adams', '57 Cedar Street', NULL, NULL, NULL, NULL, 'T7 7TT', 'emily.adams@example.com', '01234567946', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL),
    (20030, 'RE', NULL, 'Mr', 'James', NULL, NULL, 'Nelson', '58 Elm Avenue', NULL, NULL, NULL, NULL, 'U8 8UU', 'james.nelson@example.com', '01234567947', NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL, NULL, NULL)
;

-- Reset the sequence for name_address.na_id;
SELECT setval('na_seq'::regclass, (SELECT MAX(na_id)::bigint FROM name_address));

-- Create some application list entries that have 
-- AC_ID in (680,681,682,683,684,685,686,687,688,721,1082,1083,1084,1085,1086,1087,
--          1088,1089,1090,1062,1282,1842)
-- Hang them off the newly created application lists
-- Give them applicants from standard applicants
-- Give then respondents from the name_address table
INSERT INTO application_list_entries (
    ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, 
    number_of_bulk_respondents, application_list_entry_wording, 
    case_reference, account_number, entry_rescheduled, notes, version,
    changed_by, changed_date, bulk_upload, user_name, sequence_number,
    tcep_status, message_uuid, retry_count, lodgement_date)
VALUES 
    (20000, 70000, 31, 680, NULL, 20000, NULL, 'Application for a collection order to enforce the remaining balance on a financial penalty account of {100.00}','AC-EF90001', 'ACEF90001', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20001, 70001, 31, 681, NULL, 20001, NULL, 'Application for a collection order to enforce the remaining balance on a confiscation order account of {5.00}','AC-EF90002', 'ACEF90002', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20002, 70002, 31, 682, NULL, 20002, NULL, 'Application for a collection order to enforce the remaining balance on a Crown Court financial penalty account of {250.00} with payment terms as set by the Crown Court.','AC-EF90003', 'ACEF90003', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20003, 70003, 31, 683, NULL, 20003, NULL, 'Application to transfer enforcement of the financial penalty to {HAMPSHIRE}','AC-EF90004', 'ACEF90004', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20004, 70004, 31, 684, NULL, 20004, NULL, 'Application to transfer enforcement of the financial penalty to Scottish court: {GLASGOW SHERIFF COURT}','AC-EF90005', 'ACEF90005', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20005, 70005, 31, 685, NULL, 20005, NULL, 'Application to transfer enforcement of the financial penalty to Northern Ireland court: {NEWRY COURTHOUSE}','AC-EF90006', 'ACEF90006', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20006, 70006, 31, 686, NULL, 20006, NULL, 'Application for a warrant of control to collect the outstanding balance of {1,210.00}','AC-EF90007', 'ACEF90007', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20007, 70007, 31, 687, NULL, 20007, NULL, 'Application for the issue of an arrest warrant (without bail) to attend court for means enquiry to enforce the remaining balance of {nil}','AC-EF90008', 'ACEF90008', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20008, 70008, 31, 688, NULL, 20008, NULL, 'Application for the issue of an arrest warrant (with bail) to attend court for means enquiry to enforce the remaining balance of {nil}','AC-EF90009', 'ACEF90009', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20009, 70009, 31, 721, NULL, 20009, NULL, 'Application for a collection order to enforce the oustanding sum on a registered fixed penalty account of {75.00} with payment terms of 14 days.','AC-EF90011', 'ACEF90011', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20010, 70010, 31, 1082, NULL, 20010, NULL, 'Application for a warrant to authorise the use of reasonable force to execute a warrant of control. Balance: {113.11} Date imposed: {18/02/2019} Last Payment: {21/06/2019} Last enforcement: {WoC}','AC-EF90012', 'ACEF90012', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20011, 70011, 31, 1083, NULL, 20011, NULL, 'Application for a warrant to authorise the use of reasonable force to  take control of goods on a highway. Balance {121.24} Date imposed: {18/05/2019} Last payment: {25/05/2019} Last enforcement: {WoC}','AC-EF90013', 'ACEF90013', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20012, 70012, 31, 1084, NULL, 20012, NULL, 'Application for an order to extend the period of time for taking control of goods by 12 months. Balance {151.99} Date imposed {23/12/2022} Last payment: {28/12/2022} Last enforcement: {WoC}','AC-EF90014', 'ACEF90014', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20013, 70013, 31, 1085, NULL, 20013, NULL, 'Application for an order to specifying less than 7 dyas as the period of time for giving notice of enforcement prior to taking control of goods. Balance: {192.36} Date imposed: {05/01/2025} Last payment: {12/01/2025} Last enforcement: {WoC}','AC-EF90015', 'ACEF90015', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20014, 70014, 31, 1086, NULL, 20014, NULL, 'Application for an order to take control of goods outside the hours specified in Regulations, namely {08.00 to 17.00 Mon To Fri}','AC-EF90016', 'ACEF90016', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20015, 70015, 31, 1087, NULL, 20015, NULL, 'Application for authority to enter re-enter or remain on premises other than during the hours sepcified in Regulations, namely {48 HOURS/30 MINUTES}','AC-EF90017', 'ACEF90017', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20016, 70016, 31, 1088, NULL, 20016, NULL, 'Application for a warrant to enter premises at {2 Pine Street} to search for and take control of goods','AC-EF90018', 'ACEF90018', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20017, 70017, 31, 1089, NULL, 20017, NULL, 'Application for an order that goods may be sold other than by public auction, namely {private tender}, on the grounds that {no auction available}','AC-EF90019', 'ACEF90019', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20018, 70018, 31, 1090, NULL, 20018, NULL, 'Application for an order to determine the method of disposal of uncollected controlled goods','AC-EF90020', 'ACEF90020', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20019, 70019, 31, 1062, NULL, 20019, NULL, 'Application for a summons for a youth debtor to attend court for means enquiry. Balance {26.00} Date imposed: {01/10/2024} Last payment: {N/A} Last enforcement: {N/A}','AC-EF90021', 'ACEF90021', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20020, 70020, 31, 1282, NULL, 20020, NULL, 'Application for a summons for a corporate or organisation debtor to attend court for means enquiry. Balance: {1,427.51} Date imposed: {12/11/2024} Last payment: {N/A} Last enforcement: {REM}','AC-EF90022', 'ACEF90022', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP),
    (20021, 70021, 31, 1842, NULL, 20021, NULL, 'Application for a warranty of control in relation to a company to collect the outstandng balance of {247.89}','AC-EF90023', 'ACEF90023', 'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, 'N', 'TestData.Tool', 1, null, null, 0, CURRENT_TIMESTAMP)
;


-- Reset the sequence for application_list_entries.ale_id;
SELECT setval('ale_seq'::regclass, (SELECT MAX(ale_id)::bigint FROM application_list_entries));

INSERT INTO app_list_entry_official (
        aleo_id, ale_ale_id, title, forename, surname, official_type, changed_by, changed_date, user_name
) VALUES
        (20001, 20000, 'Mr', 'Adrian', 'Cross', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20002, 20000, 'Ms', 'Rebecca', 'Dalton', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20003, 20000, 'Dr', 'Nicholas', 'Everett', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20004, 20000, 'Mrs', 'Monica', 'Fairfax', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20005, 20001, 'Mr', 'Benjamin', 'Garrett', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20006, 20001, 'Ms', 'Samantha', 'Gideon', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20007, 20001, 'Dr', 'Samuel', 'Gilmore', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20008, 20001, 'Mrs', 'Rachel', 'Goodwin', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20009, 20002, 'Mr', 'Marcus', 'Grayson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20010, 20002, 'Ms', 'Natalie', 'Greer', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20011, 20002, 'Dr', 'Vincent', 'Griffin', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20012, 20002, 'Mrs', 'Olivia', 'Griffith', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20013, 20003, 'Mr', 'Felix', 'Hadley', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20014, 20003, 'Ms', 'Theresa', 'Halifax', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20015, 20003, 'Dr', 'Calvin', 'Harding', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20016, 20003, 'Mrs', 'Susan', 'Harmon', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20017, 20004, 'Mr', 'Miles', 'Hart', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20018, 20004, 'Ms', 'Helen', 'Hastings', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20019, 20004, 'Dr', 'Jasper', 'Hatfield', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20020, 20004, 'Mrs', 'Pamela', 'Hawkins', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20021, 20005, 'Mr', 'Ivan', 'Hayden', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20022, 20005, 'Ms', 'Claire', 'Hayes', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20023, 20005, 'Dr', 'Stuart', 'Haynes', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20024, 20005, 'Mrs', 'Eleanor', 'Hayward', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20025, 20006, 'Mr', 'Oscar', 'Heald', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20026, 20006, 'Ms', 'Michelle', 'Healey', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20027, 20006, 'Dr', 'Walter', 'Hearn', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20028, 20006, 'Mrs', 'Louise', 'Heath', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20029, 20007, 'Mr', 'Gregory', 'Heaven', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20030, 20007, 'Ms', 'Judith', 'Hedges', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20031, 20007, 'Dr', 'Harold', 'Hedrick', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20032, 20007, 'Mrs', 'Megan', 'Heels', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20033, 20008, 'Mr', 'Julian', 'Hefner', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20034, 20008, 'Ms', 'Cynthia', 'Hegwood', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20035, 20008, 'Dr', 'Everett', 'Heisler', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20036, 20008, 'Mrs', 'Kimberly', 'Held', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20037, 20009, 'Mr', 'Wesley', 'Helen', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20038, 20009, 'Ms', 'Denise', 'Heller', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20039, 20009, 'Dr', 'Leroy', 'Hellyer', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20040, 20009, 'Mrs', 'Patricia', 'Helms', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20041, 20010, 'Mr', 'Melvin', 'Helton', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20042, 20010, 'Ms', 'Christine', 'Helwig', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20043, 20010, 'Dr', 'Floyd', 'Hembree', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20044, 20010, 'Mrs', 'Karen', 'Hemel', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20045, 20011, 'Mr', 'Boyd', 'Heming', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20046, 20011, 'Ms', 'Joyce', 'Hemingway', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20047, 20011, 'Dr', 'Juan', 'Hemsworth', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20048, 20011, 'Mrs', 'Amanda', 'Henderson', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20049, 20012, 'Mr', 'Elton', 'Hendricks', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20050, 20012, 'Ms', 'Ruby', 'Hendrix', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20051, 20012, 'Dr', 'Clinton', 'Henley', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20052, 20012, 'Mrs', 'Wanda', 'Henning', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20053, 20013, 'Mr', 'Dwayne', 'Henrich', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20054, 20013, 'Ms', 'Dorothy', 'Henricks', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20055, 20013, 'Dr', 'Brandon', 'Hensel', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20056, 20013, 'Mrs', 'Tammy', 'Henshaw', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20057, 20014, 'Mr', 'Todd', 'Henson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20058, 20014, 'Ms', 'Theresa', 'Hepburn', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20059, 20014, 'Dr', 'Keith', 'Hepworth', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20060, 20014, 'Mrs', 'Beverly', 'Herald', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20061, 20015, 'Mr', 'Rodney', 'Herbert', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20062, 20015, 'Ms', 'Alice', 'Herbst', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20063, 20015, 'Dr', 'Wayne', 'Herd', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20064, 20015, 'Mrs', 'Nancy', 'Heredia', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20065, 20016, 'Mr', 'Lonnie', 'Herford', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20066, 20016, 'Ms', 'Ruth', 'Hermes', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20067, 20016, 'Dr', 'Jeremy', 'Hernandez', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20068, 20016, 'Mrs', 'Diane', 'Herold', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20069, 20017, 'Mr', 'Craig', 'Herrera', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20070, 20017, 'Ms', 'Margaret', 'Herrick', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20071, 20017, 'Dr', 'Erik', 'Herring', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20072, 20017, 'Mrs', 'Carol', 'Herrold', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20073, 20018, 'Mr', 'Dan', 'Herron', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20074, 20018, 'Ms', 'Joan', 'Hersey', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20075, 20018, 'Dr', 'Ethan', 'Hershey', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20076, 20018, 'Mrs', 'Gloria', 'Hertz', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20077, 20019, 'Mr', 'Zachary', 'Hertzog', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20078, 20019, 'Ms', 'Evelyn', 'Hervey', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20079, 20019, 'Dr', 'Lucas', 'Herzfeld', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20080, 20019, 'Mrs', 'Shirley', 'Herzog', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20081, 20020, 'Mr', 'Mason', 'Hess', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20082, 20020, 'Ms', 'Brenda', 'Hester', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20083, 20020, 'Dr', 'Jacob', 'Hetzel', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20084, 20020, 'Mrs', 'Deborah', 'Heuer', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20085, 20021, 'Mr', 'Alexander', 'Heusser', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20086, 20021, 'Ms', 'Anna', 'Hewes', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20087, 20021, 'Dr', 'Ethan', 'Hewett', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20088, 20021, 'Mrs', 'Marie', 'Hewit', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload')
        ;
-- Reset the sequence for app_list_entry_official.aleo_id;
SELECT setval('aleo_seq'::regclass, (SELECT MAX(aleo_id)::bigint FROM app_list_entry_official));

-- Insert our test data for V31
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('31', 'test_support', 'check_data_expected_v31_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v31_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Check for application_codes with EF prefix
    IF NOT EXISTS (SELECT 1 FROM application_codes WHERE application_code LIKE 'EF%') THEN
        RAISE EXCEPTION 'No application codes with EF prefix found';
    END IF;

    -- Check for specific EF application codes inserted in this script
    IF NOT EXISTS (SELECT 1 FROM application_codes WHERE application_code IN ('EF99001', 'EF99002', 'EF99003', 'EF99004', 'EF99005', 'EF99006', 'EF99007', 'EF99008', 'EF99009', 'EF99011', 'EF99012', 'EF99013', 'EF99014', 'EF99015', 'EF99016', 'EF99017', 'EF99018', 'EF99019', 'EF99020', 'EF99021', 'EF99022', 'EF99023')) THEN
        RAISE EXCEPTION 'Expected EF application codes are missing';
    END IF;

    -- Check for application_lists entries 70000 to 70021
    IF NOT EXISTS (SELECT 1 FROM application_lists WHERE al_id BETWEEN 70000 AND 70021) THEN
        RAISE EXCEPTION 'Application lists with al_id between 70000 and 70021 are missing';
    END IF;

    -- Check for name_address entries between 20000 and 20030
    IF NOT EXISTS (SELECT 1 FROM name_address WHERE na_id BETWEEN 20000 AND 20030) THEN
        RAISE EXCEPTION 'Name address entries with na_id between 20000 and 20030 are missing';
    END IF;

    -- Check for application_list_entries created between 20000 and 20021
    IF NOT EXISTS (SELECT 1 FROM application_list_entries WHERE ale_id BETWEEN 20000 AND 20021) THEN
        RAISE EXCEPTION 'Application list entries created between 20000 and 20021 are missing';
    END IF;

    -- Check for app_list_entry_official with aleo_id between 20001 and 20088
    IF NOT EXISTS (SELECT 1 FROM app_list_entry_official WHERE aleo_id BETWEEN 20001 AND 20088) THEN
        RAISE EXCEPTION 'App list entry official with aleo_id between 20001 and 20088 is missing';
    END IF;

	-- If all checks pass, do nothing (test passes)
END $$;


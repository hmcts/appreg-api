INSERT INTO petty_sessional_areas(psa_id, psa_name, short_name, version_number, changed_by,
                                  changed_date, cma_cma_id, psa_code, start_date, end_date, jc_name, court_type, crime_cases_loc_id, fine_accounts_loc_id,
                                  maintenance_enforcement_loc_id, family_cases_loc_id, court_location_code, central_finance_loc_id, sl_psa_name, norg_id)
VALUES
    (1, 'psa name', 'shortname', 0.0, 0, '1904-01-01', 0, 1111, '2007-10-01', NULL, 'jc_name', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name', 0);


--
-- ----------------------- COURTHOUSE -----------------------
--
INSERT INTO national_court_houses (nch_id, courthouse_name, version_number, changed_by, changed_date, court_type, start_date, end_date, loc_loc_id, psa_psa_id, court_location_code, sl_courthouse_name, norg_id)
VALUES
    (1, 'Royal Courts of Justice', 0.0, 0, '1987-05-01', 'Appeal', '1875-12-04', NULL, 501, 1, 'RCJ001', 'RCJ', 701),
    (2, 'Manchester Civil Justice Centre', 0.0, 0, '1987-05-01', 'Court', '2007-10-01', NULL, 502, 1, 'MCJC002', 'MCJC', 702),
    (3, 'Cardiff Crown Court', 0.0, 0, '1987-05-01', 'Court', '1904-01-01', NULL, 503, 1, 'CCC003', 'CCC', 703),
    (4, 'Birmingham Civil Justice Centre', 0.0, 0, '1987-05-01','Court', '1987-05-01', NULL, 504, 1, 'BCJC004', 'BCJC', 704),
    (5, 'Leeds Combined Court Centre',  0.0, 0, '1987-05-01', 'Court', '1993-09-15', NULL, 505, 1, 'LCCC005', 'LCCC', 705),
    (6, 'Bristol Crown Court', 0.0, 0, '1987-05-01', 'Court', '1993-06-01', NULL, 506, 1, 'BCC006', 'BCC', 706),
    (7, 'Liverpool Crown Court', 0.0, 0, '1987-05-01', 'Court', '1984-10-01', NULL, 507, 1, 'LCC007', 'LCC', 707),
    (8, 'Nottingham Justice Centre',  0.0, 0, '1987-05-01', 'Court', '1996-04-01', NULL, 508, 1, 'NJC008', 'NJC', 708),
    (9, 'Sheffield Combined Court Centre', 0.0, 0, '1987-05-01', 'Court', '1995-07-01', NULL, 509, 1, 'SCCC009', 'SCCC', 709),
    (10, 'Newcastle Crown Court',  0.0, 0, '1987-05-01', 'Court', '1990-03-01', NULL, 510, 1, 'NCC010', 'NCC', 710);
--
--
--
----------------------- APPLICATION FEE -----------------------
--
-- CO1.1 - Main Fee and Offset Fee.
INSERT INTO fee (fee_id, fee_reference, fee_description, fee_value, fee_start_date, fee_end_date, fee_version, fee_changed_by, fee_changed_date, fee_user_name, is_offsite) VALUES
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'JP perform function away from court', 50.00, DATE '2014-07-25',  '2019-07-25', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'JP perform function away from court', 100.00, DATE '2022-07-25',  NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'JP perform function away from court', 200.00, DATE '2022-07-25',  NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'Offsite: JP perform function away from court', 30.00, DATE '2021-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'Offsite: JP perform function away from court', 30.00, DATE '2021-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'Offsite: JP perform function away from court', 40.00, DATE '2021-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                (nextval('fee_seq'), 'CO1.1', 'Offsite: JP perform function away from court', 70.00, DATE '2015-07-25', '2020-07-25', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO2.1 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.1', 'Application to state a case for the High Court', 515.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.1', 'Application to state a case for the High Court', 155.00, CURRENT_TIMESTAMP, NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO2.2 - Main Fee only.
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.2', 'Appeal against DEO – Child Support Act 1991', 100.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                -- CO2.3 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.3', 'Appeal under Sch 5 Licensing Act 2003', 410.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.3', 'Appeal under Sch 5 Licensing Act 2003', 70.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO2.4 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.4', 'Other appeal where no fee specified', 205.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO2.4', 'Other appeal where no fee specified', 70.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO3.1 - Main Fee only.
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.1', 'Certificate of refusal to state a case.', 105.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                -- CO3.2 - Main Fee only.
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.2', 'Certificate of satisfaction under Register of judgments, orders and fines', 15.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                -- CO3.3 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.3', 'Certified copy of a memorandum of conviction.', 60.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.3', 'Certified copy of a memorandum of conviction.', 60.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO3.4 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.4', 'Certificate or certified document where no other fee is specified.', 60.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO3.4', 'Certificate or certified document where no other fee is specified.', 25.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO4.1 - Main Fee and Offset Fee.
                                                                                                                                                                                (nextval('fee_seq'), 'CO4.1', 'Liability Order – Council Tax and NDR', 325.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false),
                                                                                                                                                                                (nextval('fee_seq'), 'CO4.1', 'Liability Order – Council Tax and NDR', 0.50, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', true),
                                                                                                                                                                                -- CO4.2 - Main Fee only.
                                                                                                                                                                                (nextval('fee_seq'), 'CO4.2', 'Liability Order – Child Support Act 1991', 40.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload', false);
--
--
--

INSERT INTO criminal_justice_area (cja_id, cja_code, cja_description) VALUES
    (1, 'CJ', 'CJA_DESCRIPTION'),
    (2, 'CJ', 'CJA_DESCRIPTION'),
    (3, 'CD', 'CJA_CD_DESCRIPTION'),
    (4, 'CE', 'CJA_CE_DESCRIPTION');



----------------------- APPLICATION LIST -----------------------
--

INSERT INTO application_lists (al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse, list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id)
VALUES
    (1,'OPEN', '2024-04-21', '2024-04-21:10:00:00', 'RCJ001', null, 'Morning list for Family Court', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Royal Courts of Justice', 6, 3, 1),
    (2,'OPEN', '2025-04-21', '2024-04-21:14:00', 'MCJC002', null, 'Afternoon list for Civil Court', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Manchester Civil Justice Centre', 6, 3, 1),
    (3,'CLOSED', '2025-04-22', '2025-04-22:09:00',  'CCC003', null ,'Cancelled hearing for Probate', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Cardiff Crown Court', 6, 3, 1),
    (4,'CLOSED', '2025-04-19', '2025-04-19:11:30',  'BCJC004', null, 'Completed list - criminal matters', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Birmingham Civil Justice Centre',  6, 3, 1),
    (5,'OPEN', '2025-04-23', '2025-04-23:15:00',  'LCCC005', null, 'Immigration list', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Leeds Combined Court Centre',  6, 3, 1),
    (6,'OPEN', '2025-04-24', '2025-04-24:13:00',  'BCC006', null, 'Tribunal cases', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000002', 'Bristol Crown Court', 6, 3, 1),
    (7,'OPEN', '2025-04-25', '2025-04-25:16:00',  'LCC007', null, 'Urgent list', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000002', 'Liverpool Crown Court', 6, 3, 1),
    (8,'CLOSED', '2025-04-18', '2025-04-18:08:30',  'NJC008',null,'No show', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000002', 'Nottingham Justice Centre', 6, 3, 1),
    (9,'CLOSED', '2025-04-17', '2025-04-17:12:00',  'SCCC009', null, 'Family cases', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000003', 'Sheffield Combined Court Centre', 6, 3, 1),
    (10,'OPEN', '2025-04-26', '2025-04-26:09:30',  'NCC010', null, 'New applications', 1, 0, CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000003', 'Newcastle Crown Court', 6, 3, 1);
--
--
--
-- ----------------------- APPLICATION CODES -----------------------
--
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
    (1, 'AD99001','Copy documents','Request to copy documents','',1,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1'),
    (2, 'AD99002','Copy documents (electronic)','Request for copy documents on computer disc or in electronic form','',1,0,'address1@cgi.com','address2@cgi.com',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00', 'admin', 'CO1.1'),
    (3, 'AD99003','Extract from the Court Register','Certified extract from the court register','',1,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin','CO1.1'),
    (4, 'AD99004','Certificate of Satisfaction','Request for a certificate of satisfaction of debt registered in the register of judgements, orders and fines','',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (5, 'AD99005','Certified genuine copy document','Request for a copy of a document certified as a genuine copy of the original document','',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (6, 'AP99001','Appeal to Crown Court','Notice of appeal in respect of a case heard on {TEXT|Date of Hearing|10}','Section 108 Magistrates'' Courts Act 1980',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,'2016-01-01 00:00:00','admin',NULL),
    (7, 'AP99002','Appeal by Case Stated (Crime)','Notice of appeal to the High Court by way of case stated in respect of a criminal case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (8, 'AP99003','Appeal by Case Stated (Civil)','Notice of appeal to the High Court by way of case stated in respect of case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (9, 'AP99004','Request for Certificate of Refusal to State a Case (Civil)','Request for a certificate of refusal to state a case for the opinion of the High Court in respect of civil proceedings heard on {TEXT|Date|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (10, 'CT99001','Issue of liability order summons -council tax (bulk)','Attends to swear a complaint for the issue summonses for the debtors to answer an application for a liability order in relation to unpaid council tax (number of cases {TEXT|Number|4})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (11, 'CT99002','Issue of liability order summons - council tax','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid council tax (reference {TEXT|Reference|100})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (12, 'CT99003','Issue of liability order summons -non-domestic rate (bulk)','Attends to swear a complaint for the issue summonses for the debtors to answer an application for a liability order in relation to unpaid non-domestic rate (number of cases {TEXT|Number|4})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,1,0, 0, '2016-01-01 00:00:00','admin',NULL),
    (13, 'CT99004','Issue of liability order summons - non-domestic rate','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid non-domestic rate (reference {TEXT|Reference|100})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (14, 'CT99005','Issue of warrant of arrest in commitment proceedings - council tax (bulk)','Attends to swear a complaint for the issue of warrants of arrest for the debtors to answer an application for committal to prison (number of cases {TEXT|Number|4})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (15, 'CT99006','Issue of warrant of arrest in commitment proceedings - council tax','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for committal to prison in relation to unpaid council tax (reference {TEXT|Reference|100})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0,0, 0, '2016-01-01 00:00:00','admin',NULL),
    (16, 'CT99007','Issue of warrant of arrest in commitment proceedings - non-domestic rate (bulk)','Attends to swear a complaint for the issue of warrants of arrest for the debtors to answer an application for commitment to prison in relation to unpaid non-domestic rate (number of cases {TEXT|Number|4})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (17, 'CT99008','Issue of warrant of arrest in commitment proceedings - non-domestic rate','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for commitment to prison in relation to unpaid non-domestic rate (reference {TEXT|Reference|100})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (18, 'CT99009','Issue of liability order summons - child support','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid child support (reference {TEXT|Reference|100})','Regulation 28 of the Child Support (Collection and Enforcement) Regulations 1992',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,'2016-01-01 00:00:00','admin',NULL),
    (19, 'CT99010','Issue of liability order warrant in commitment proceedings - child support','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for committal to prison in relation to unpaid child support (reference {TEXT|Reference|100})','Regulation 28 of the Child Support (Collection and Enforcement) Regulations 1992',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (20, 'MS99001','Special Constable''s Attestation','Attends to swear oath of the office of Special Constable','Section 29 Police Act 1996',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
    (21, 'MS99002','Change of name','Attends to make a statutory declaration that henceforth the applicant will be known as {TEXT|New Name|100}','Section 18 Statutory Declarations Act 1835',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0,0, 0, '2016-01-01 00:00:00', 'admin', NULL),
    (22, 'MS99003','Statutory Decalration - Local Authority Car Park','Attends to make a statutory declaration in relation to car park penalty issued on {TEXT|Date|10} for vehicle {TEXT|Vehicle Reg|10}','Section 18 Statutory Declarations Act 1835',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (23, 'MS99004','Statutory Decalration -Lost documents','Attends to make a statutory declaration in relation to loss of original document, namely {TEXT|Specify Document Lost|100}','Section 18 Statutory Declarations Act 1835',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (24, 'RE99001','Rights of Entry Warrant - Gas Operator','Application by a gas operator for a warrant to enter premises at {TEXT|Premises Address|200}','Section 2 Rights of Entry (Gas and Electricity Boards) Act 1954',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (25, 'RE99002','Rights of Entry Warrant - Electricity Operator','Application by an electricity operator for a warrant to enter premises at {TEXT|Premises Address|200}','Section 2 Rights of Entry (Gas and Electricity Boards) Act 1954',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
    (26, 'RE99003','Rights of Entry Warrant - Food Safety Regs','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Reg 16 Food Safety and Hygiene (England) Regulations 2013',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (27, 'RE99004','Rights of Entry Warrant - Food Safety Act 1990','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Section 32 of the Food Safety Act 1990',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (28, 'RE99005','Rights of Entry Warrant - dairy premises','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Reg 9 Single Common Market Organisation (Emergency Aid for Milk Producers) Regulations 2015',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (29, 'SW99001','Search Warrant - Stolen Goods','Application for a search warrant in respect of stolen goods under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (30, 'SW99002','Search Warrant - Controlled Drugs','Application for a search warrant in respect of controlled drugs reference under number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0,0, 0, '2016-01-01 00:00:00', 'admin',NULL),
    (31, 'SW99003','Search Warrant - Psychoactive Substance','Application for a search warrant in respect of psychoactive substances under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (32, 'SW99004','Search Warrant - Firearms','Application for a search warrant in respect of firearms under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
    (33, 'SW99005','Search Warrant - Evidential Material','Application for a search warrant in respect of evidential material under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
    (34, 'SW99006','Search Warrant - Mental Health','Application for a warrant to search for a person and if needs be remove to a place of safety.','Section 135 of the Mental Health Act 1983',1,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
    (35, 'SW99007','Inspection of Bankers'' Books (criminal proceedings)','Application for an order to allow the applicant to inspect or take copies of bankers books held by {TEXT|Name of Bank|100} in respect of criminal proceedings at {TEXT|Name of Court|100}.','Section 7 Bankers''' || ' Books Evidence Act 1879',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
    (36, 'SW99008','Inspection of Bankers'' Books (civil or other proceedings)','Application for an order to allow the applicant to inspect or take copies of bankers books held by {TEXT|Name of Bank|100} in respect of civil or other proceedings at {TEXT|Name of Court|100}.','Section 7 Bankers''' || ' Books Evidence Act 1879',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
    (37, 'SW99009','Approval of Access to Communications Data','Approval of an authorisation issued by the applicant for access to communications data reference {TEXT|Reference|100}','Section 23A Regulation of Investigatory Powers Act 2000',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
    (38, 'SW99010','Approval of Use of Directed Surveillance','Approval of an authorisation issued by the applicant for the use of directed surveillance reference {TEXT|Reference|100}','Sections 28 and 32A Regulation of Investigatory Powers Act 2000',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
    (39, 'SW99011','Approval of Use of Covert Human Intelligence Source','Approval of an authorisation issued by the applicant for the use of covert human intelligence sources reference {TEXT|Reference|100}','Sections 29 and 32A Regulation of Investigatory Powers Act 2000',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
    (40, 'MS99005','Statutory Decalration - Non-standard Civil','Attends to make a statutory declaration in relation to  {TEXT|Specify Nature of Declaration|200}','Section 18 Statutory Declarations Act 1835',0,0,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', NULL),
    (41, 'MS99006','Condemnation of Unfit Food','Application for the condemnation of food, namely  {TEXT|Describe Seized Food|100}','Section 9 Food Safety Act 1990',0,1,'','',TIMESTAMP '2016-01-01 00:00:00',NULL,0, 0, 0,'2016-01-01 00:00:00', 'admin', NULL),
    (42, 'AD99002','Copy documents (electronic)','Request for copy documents on computer disc or in electronic form','',1,0,'address1@cgi.com','address2@cgi.com',TIMESTAMP '2016-01-01 00:00:00',CURRENT_TIMESTAMP,0, 0, 0,  '2016-01-01 00:00:00', 'admin', 'CO1.1');

--
--
--
------------------------- IDENTITY_DETAILS -----------------------
--
INSERT INTO name_address (na_id, code, name, title, forename_1, forename_2, forename_3, surname, address_l1, address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number, version, changed_by, changed_date, user_name, date_of_birth, dms_id)
VALUES
    (1, 'RE', 'Jack Turner', 'Mr', 'John', NULL, NULL, 'Turner', '1 Market Street', NULL, NULL, NULL, NULL, 'AB11 2CD', 'john.smith@example.com', '01234567890', NULL,  0, 0, TIMESTAMP '2016-01-01 00:00:00' , NULL, NULL, NULL),
    (2, 'RE', 'Sarah Johnson', 'Mrs', 'Sarah', NULL, NULL, 'Johnson', '12 The Avenue', NULL, NULL, NULL, NULL, 'XY9 8ZZ', 's.johnson@example.com', NULL, '07700900000',  0, 0, TIMESTAMP' 2016-01-01 00:00:00' , NULL, NULL, NULL),
    (3, 'RE', 'Sam Burton', NULL, 'Sam', NULL, NULL, 'Burton', 'Flat 4, 22 Hillside', NULL, NULL, NULL, NULL, 'SN12 1ZZ', NULL, NULL, NULL, 0, 0, '2016-01-01 00:00:00' , NULL, NULL, NULL),
    (4, 'AP', 'Legal Aid Board', NULL, NULL, NULL, NULL, NULL, '100 Legal Street', NULL, NULL, NULL, NULL, 'BA15 1LA', 'info@legalaid.example.com', NULL, NULL, 0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL, NULL, NULL);
--
--
--
-- ----------------------- STANDARD_APPLICANT -----------------------
--
INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date,
                                 version,  changed_by, changed_date, user_name, name, title, forename_1, forename_2, forename_3, surname, address_l1,
                                 address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number) VALUES
                                                                                                                                               (1, 'APP001', CURRENT_TIMESTAMP, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000001', 'John Smith', 'Mr', 'John', NULL, NULL, 'Smith','123 High Street', NULL, NULL, 'Townsville', NULL,'TS1 1AB', 'john.smith@example.com', '01234567890', '07123456789'),
                                                                                                                                               (2, 'APP002', CURRENT_TIMESTAMP, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000001',  'Jane Doe', 'Ms', 'Jane', NULL, NULL, 'Doe','456 Elm Road', 'Apt 5', NULL, 'Cityville', NULL,'CV2 2BC', 'jane.doe@example.com', '02345678901', NULL),
                                                                                                                                               (3, 'APP003',  CURRENT_TIMESTAMP, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000001',  'Alex Dunn', 'Dr', 'Alex', 'Taylor', NULL, 'Dunn','789 Oak Avenue', NULL, NULL, 'Villageham', 'Countyshire','VH3 3CD', 'alex.johnson@example.com', NULL, '07987654321');
--
--
--
-- ----------------------- APPLICATIONS -----------------------
INSERT INTO application_list_entries (ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents, application_list_entry_wording, case_reference, entry_rescheduled, notes, version, changed_by, changed_date, bulk_upload, sequence_number, lodgement_date) VALUES
                                                                                                                                                                                                                                                                                         (1, 1, 1, 1, NULL,NULL, 1, 'Request to copy documents', 'CASE123456', 'N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', TIMESTAMP '2021-01-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (2, 1, 2, 2, NULL, 2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs',1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2025-05-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (3, 1, 2, 3, NULL,4, 0,'Court register extract application', 'CASE123458', 'N', NULL, 1, 1,CURRENT_TIMESTAMP, 'N', 1, TIMESTAMP '2022-01-30 10:00:00'),
                                                                                                                                                                                                                                                                                         (4, 1, 1, 4, NULL,1,0,'Application to copy documents (Standard)', 'CASE123456','N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', TIMESTAMP '2021-01-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (5, 2, 2, 5, NULL,2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs', 1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2023-01-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (6, 2, 2, 6, NULL, 3, 4, 'Court register extract application', 'CASE123458','N', NULL, 1, 1,CURRENT_TIMESTAMP, 'N', 1, TIMESTAMP '2002-01-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (7, 2, 1, 7, NULL,1,0,'Application to copy documents (Standard)', 'CASE123456', 'N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', TIMESTAMP '2024-04-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (8, 2, 2, 8, NULL,2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs', 1, 1,CURRENT_TIMESTAMP, 'Y', '1', TIMESTAMP '2006-02-01 00:00:00'),
                                                                                                                                                                                                                                                                                         (9, 2, 2, 9, NULL,4, 0,'Court register extract application', 'CASE123458', 'N', NULL, 1, 1,CURRENT_TIMESTAMP,'N', '1', TIMESTAMP '2006-02-01 00:00:00');

--
--
--
-- ----------------------- APPLICATION_FEE_RECORD -----------------------
INSERT INTO app_list_entry_fee_status (alefs_id, alefs_ale_id, alefs_payment_reference, alefs_fee_status, alefs_fee_status_date,  alefs_version, alefs_changed_by, alefs_changed_date, alefs_user_name, alefs_status_creation_date) VALUES
    (1, 1, 'TESTPAY123', 'D', DATE '2025-05-15', 1, 1,  DATE '2025-05-15', 'AR4.Initial.SQL.Upload', DATE '2025-05-15');
--
--
-- ----------------------- RESULT_CODE -----------------------
--
INSERT INTO resolution_codes (rc_id, resolution_code, resolution_code_title, resolution_code_wording, resolution_legislation,
                              rc_destination_email_address_1, rc_destination_email_address_2, resolution_code_start_date, resolution_code_end_date,
                              version,changed_by, changed_date, user_name) VALUES
                                                                               (1, 'APPC', 'Appeal to Crown Court', 'Appeal forwarded to {TEXT|Name of Crown Court|100}.', NULL, NULL, NULL, DATE '2016-01-01', NULL, 0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (2, 'AUTH', 'Authorised', 'Authorised.', NULL, NULL, NULL, DATE '2016-01-01', NULL, 0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (3, 'CASE', 'Case Stated', 'Court agrees to state a case for the opinion of the High Court.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (4, 'COL', 'Collection Order', 'Collection order made.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (5, 'FRB', 'Fee Remitted (Benefits)', 'Fee remitted as the applicant is in receipt of passported benefits.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (6, 'FRO', 'Fee Remitted (Other)', 'Fee remitted. Reason: {TEXT|Reason text|100}.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (8, 'LEX', 'DVLA notification', 'DVLA to be notified.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (9, 'REF', 'Refused', 'Refused.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (10, 'RESP', 'Respondent Attends', 'Respondent attended.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (11, 'RSN', 'Reasons', 'Reasons: {TEXT|Reason text|100}.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (12, 'RTC', 'Refer to Court', 'Referred for full court hearing on {TEXT|Date|10} at {TEXT|Courthouse|100}.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (13, 'SDA', 'Statutory Declaration', 'Statutory declaration accepted.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (14, 'SSI', 'Summons Issued', 'Summons issued.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (15, 'SSIB', 'Summons Issued (multiple)', '{TEXT|Number|4} summons issued.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (16, 'TFO', 'Trasfer of Fine Order', 'Fine enforcement transferred.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (17, 'TFOS', 'Tranfer or Fine Order (Scotland)', 'Fine enforcement transferred to {TEXT|Scottish Court Name|100}.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (18, 'WDN', 'Withdrawn', 'Application withdrawn.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (19, 'WDNF', 'Withdrawn (Fee remitted)', 'Application withdrawn. Fee to be remitted.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (20, 'WOC', 'Warrant of Control', 'Warrant of control issued.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (21, 'WTI', 'Warrant Issued', 'Arrest warrant issued.', NULL, NULL, NULL, DATE '2016-01-01', NULL,0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL),
                                                                               (22, 'WTIB', 'Warrants issued (multiple)', '{TEXT|Number|4} arrest warrants issued.', NULL, NULL, NULL, DATE '2016-01-01', NULL, 0, 0, TIMESTAMP '2016-01-01 00:00:00', NULL);

--
--
--
-- ----------------------- APPLICATION_RESULT -----------------------
--
INSERT INTO app_list_entry_resolutions (aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording, al_entry_resolution_officer, version, changed_by, changed_date, user_name)  VALUES
                                                                                                                                                                                    (1,1,1,'Application granted in full.','Magistrate Jane Doe',1,1,CURRENT_TIMESTAMP, NULL),
                                                                                                                                                                                    (2,2,1, 'Refused due to lack of supporting documents.','Magistrate John Smith',1,1,CURRENT_TIMESTAMP, NULL);

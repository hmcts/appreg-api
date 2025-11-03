-- History
-- June 2025 Initial Version
-- Matthew Harman       V1.1    11/09/2025      Expanded data set
-- Matthew Harman       V2.0    16/09/2025	Added code for testing script applied
-- Matthew Harman       V3.0    02/10/2025	Modified data for changed_by to be varchar(73)
-- Matthew Harman       V4.0    13/10/2025      Modified data for Name_Address as changed_by now varchar(73)
-- Matthew Harman       V5.0    13/10/2025      Modified data for Application_Lists as date and time fields changed
-- Matthew Harman       V6.0    22/10/2025      Modified data for ARCPOC-647
--
-- ----------------------- PETTY SESSIONAL AREA -----------------------
--
-- 10 sets of records
INSERT INTO petty_sessional_areas(psa_id, psa_name, short_name, version_number, changed_by,
                                  changed_date, cma_cma_id, psa_code, start_date, end_date, jc_name, court_type, crime_cases_loc_id, fine_accounts_loc_id,
                                  maintenance_enforcement_loc_id, family_cases_loc_id, court_location_code, central_finance_loc_id, sl_psa_name, norg_id)
VALUES
        (1, 'psa name1', 'short1', 0.0, 0, '1904-01-01', 0, 1111, '2007-10-01', NULL, 'jc_name1', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name1', 0),
        (2, 'psa name2', 'short2', 0.0, 0, '1904-01-01', 0, 2222, '2007-10-01', NULL, 'jc_name2', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name2', 0),
        (3, 'psa name3', 'short3', 0.0, 0, '1904-01-01', 0, 3333, '2007-10-01', NULL, 'jc_name3', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name3', 0),
        (4, 'psa name4', 'short4', 0.0, 0, '1904-01-01', 0, 4444, '2007-10-01', NULL, 'jc_name4', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name4', 0),
        (5, 'psa name5', 'short5', 0.0, 0, '1904-01-01', 0, 5555, '2007-10-01', NULL, 'jc_name5', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name5', 0),
        (6, 'psa name6', 'short6', 0.0, 0, '1904-01-01', 0, 6666, '2007-10-01', NULL, 'jc_name6', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name6', 0),
        (7, 'psa name7', 'short7', 0.0, 0, '1904-01-01', 0, 7777, '2007-10-01', NULL, 'jc_name7', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name7', 0),
        (8, 'psa name8', 'short8', 0.0, 0, '1904-01-01', 0, 8888, '2007-10-01', NULL, 'jc_name8', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name8', 0),
        (9, 'psa name9', 'short9', 0.0, 0, '1904-01-01', 0, 9999, '2007-10-01', NULL, 'jc_name9', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name9', 0),
        (10, 'psa name10', 'short10', 0.0, 0, '1904-01-01', 0, 1010, '2007-10-01', NULL, 'jc_name10', 'type', 0, 0, 0, 0, 'code', 0, 'sl_name10', 0);


--
-- ----------------------- COURTHOUSE -----------------------
--
-- 10 sets of 10 court houses (100 records)
INSERT INTO national_court_houses (nch_id, courthouse_name, version_number, changed_by, changed_date, court_type, start_date, end_date, loc_loc_id, psa_psa_id, court_location_code, sl_courthouse_name, norg_id)
VALUES
        (1, 'Royal Courts of Justice Set 1', 0.0, 0, '1987-05-01', 'CHOA', '1875-12-04', NULL, 501, 1, 'RCJ001', 'RCJ1', 701),
        (2, 'Manchester Civil Justice Centre Set 1', 0.0, 0, '1987-05-01', 'CHOF', '2007-10-01', NULL, 502, 1, 'MCJC002', 'MCJC1', 702),
        (3, 'Cardiff Crown Court Set 1', 0.0, 0, '1987-05-01', 'VENUE', '1904-01-01', NULL, 503, 1, 'CCC003', 'CCC1', 703),
        (4, 'Birmingham Civil Justice Centre Set 1', 0.0, 0, '1987-05-01','CHOY', '1987-05-01', NULL, 504, 1, 'BCJC004', 'BCJC1', 704),
        (5, 'Leeds Combined Court Centre Set 1',  0.0, 0, '1987-05-01', 'CHOA', '1993-09-15', '2025-01-01', 505, 1, 'LCCC005', 'LCCC1', 705),
        (6, 'Bristol Crown Court Set 1', 0.0, 0, '1987-05-01', 'CHOF', '1993-06-01', '2025-01-01', 506, 1, 'BCC006', 'BCC1', 706),
        (7, 'Liverpool Crown Court Set 1', 0.0, 0, '1987-05-01', 'VENUE', '1984-10-01', '2025-01-01', 507, 1, 'LCC007', 'LCC1', 707),
        (8, 'Nottingham Justice Centre Set 1',  0.0, 0, '1987-05-01', 'CHOY', '1996-04-01', '2025-01-01', 508, 1, 'NJC008', 'NJC1', 708),
        (9, 'Sheffield Combined Court Centre Set 1', 0.0, 0, '1987-05-01', 'CHOA', '1995-07-01', NULL, 509, 1, 'SCCC009', 'SCCC1', 709),
        (10, 'Newcastle Crown Court Set 1',  0.0, 0, '1987-05-01', 'CHOF', '1990-03-01', NULL, 510, 1, 'NCC010', 'NCC1', 710),
        (11, 'Royal Courts of Justice Set 2', 0.0, 0, '1987-05-01', 'VENUE', '1875-12-04', NULL, 511, 2, 'RCJ011', 'RCJ2', 711),
        (12, 'Manchester Civil Justice Centre Set 2', 0.0, 0, '1987-05-01', 'CHOY', '2007-10-01', NULL, 512, 2, 'MCJC012', 'MCJC2', 712),
        (13, 'Cardiff Crown Court Set 2', 0.0, 0, '1987-05-01', 'CHOA', '1904-01-01', '2025-01-01', 513, 2, 'CCC013', 'CCC2', 713),
        (14, 'Birmingham Civil Justice Centre Set 2', 0.0, 0, '1987-05-01','CHOF', '1987-05-01', '2025-01-01', 514, 2, 'BCJC014', 'BCJC2', 714),
        (15, 'Leeds Combined Court Centre Set 2',  0.0, 0, '1987-05-01', 'VENUE', '1993-09-15', '2025-01-01', 515, 2, 'LCCC015', 'LCCC2', 715),
        (16, 'Bristol Crown Court Set 2', 0.0, 0, '1987-05-01', 'CHOY', '1993-06-01', '2025-01-01', 516, 2, 'BCC016', 'BCC2', 716),
        (17, 'Liverpool Crown Court Set 2', 0.0, 0, '1987-05-01', 'CHOA', '1984-10-01', NULL, 517, 2, 'LCC017', 'LCC2', 717),
        (18, 'Nottingham Justice Centre Set 2',  0.0, 0, '1987-05-01', 'CHOF', '1996-04-01', NULL, 518, 2, 'NJC018', 'NJC2', 718),
        (19, 'Sheffield Combined Court Centre Set 2', 0.0, 0, '1987-05-01', 'VENUE', '1995-07-01', NULL, 519, 2, 'SCCC019', 'SCCC2', 719),
        (20, 'Newcastle Crown Court Set 2',  0.0, 0, '1987-05-01', 'CHOY', '1990-03-01', NULL, 520, 3, 'NCC020', 'NCC2', 720),
        (21, 'Royal Courts of Justice Set 3', 0.0, 0, '1987-05-01', 'CHOA', '1875-12-04', '2025-01-01', 521, 3, 'RCJ021', 'RCJ3', 721),
        (22, 'Manchester Civil Justice Centre Set 3', 0.0, 0, '1987-05-01', 'CHOF', '2007-10-01', '2025-01-01', 522, 3, 'MCJC022', 'MCJC3', 722),
        (23, 'Cardiff Crown Court Set 3', 0.0, 0, '1987-05-01', 'VENUE', '1904-01-01', '2025-01-01', 523, 3, 'CCC023', 'CCC3', 723),
        (24, 'Birmingham Civil Justice Centre Set 3', 0.0, 0, '1987-05-01','CHOY', '1987-05-01', '2025-01-01', 524, 3, 'BCJC024', 'BCJC3', 724),
        (25, 'Leeds Combined Court Centre Set 3',  0.0, 0, '1987-05-01', 'CHOA', '1993-09-15', NULL, 525, 3, 'LCCC025', 'LCCC3', 725),
        (26, 'Bristol Crown Court Set 3', 0.0, 0, '1987-05-01', 'CHOF', '1993-06-01', NULL, 526, 3, 'BCC026', 'BCC3', 726),
        (27, 'Liverpool Crown Court Set 3', 0.0, 0, '1987-05-01', 'VENUE', '1984-10-01', NULL, 527, 3, 'LCC027', 'LCC3', 727),
        (28, 'Nottingham Justice Centre Set 3',  0.0, 0, '1987-05-01', 'CHOY', '1996-04-01', NULL, 528, 3, 'NJC028', 'NJC3', 728),
        (29, 'Sheffield Combined Court Centre Set 3', 0.0, 0, '1987-05-01', 'CHOA', '1995-07-01', '2025-01-01', 529, 3, 'SCCC029', 'SCCC3', 729),
        (30, 'Newcastle Crown Court Set 3',  0.0, 0, '1987-05-01', 'CHOF', '1990-03-01', '2025-01-01', 530, 3, 'NCC030', 'NCC3', 730),
        (31, 'Royal Courts of Justice Set 4', 0.0, 0, '1987-05-01', 'VENUE', '1875-12-04', '2025-01-01', 531, 4, 'RCJ031', 'RCJ4', 731),
        (32, 'Manchester Civil Justice Centre Set 4', 0.0, 0, '1987-05-01', 'CHOY', '2007-10-01', '2025-01-01', 532, 4, 'MCJC032', 'MCJC4', 732),
        (33, 'Cardiff Crown Court Set 4', 0.0, 0, '1987-05-01', 'CHOA', '1904-01-01', NULL, 533, 4, 'CCC033', 'CCC4', 733),
        (34, 'Birmingham Civil Justice Centre Set 4', 0.0, 0, '1987-05-01','CHOF', '1987-05-01', NULL, 534, 4, 'BCJC034', 'BCJC4', 734),
        (35, 'Leeds Combined Court Centre Set 4',  0.0, 0, '1987-05-01', 'VENUE', '1993-09-15', NULL, 535, 4, 'LCCC0035', 'LCCC4', 735),
        (36, 'Bristol Crown Court Set 4', 0.0, 0, '1987-05-01', 'CHOY', '1993-06-01', NULL, 536, 4, 'BCC036', 'BCC4', 736),
        (37, 'Liverpool Crown Court Set 4', 0.0, 0, '1987-05-01', 'CHOA', '1984-10-01', '2025-01-01', 537, 4, 'LCC037', 'LCC4', 737),
        (38, 'Nottingham Justice Centre Set 4',  0.0, 0, '1987-05-01', 'CHOF', '1996-04-01', '2025-01-01', 538, 4, 'NJC038', 'NJC4', 738),
        (39, 'Sheffield Combined Court Centre Set 4', 0.0, 0, '1987-05-01', 'VENUE', '1995-07-01', '2025-01-01', 539, 4, 'SCCC039', 'SCCC4', 739),
        (40, 'Newcastle Crown Court Set 4',  0.0, 0, '1987-05-01', 'CHOY', '1990-03-01', '2025-01-01', 540, 4, 'NCC040', 'NCC4', 740),
        (41, 'Royal Courts of Justice Set 5', 0.0, 0, '1987-05-01', 'CHOA', '1875-12-04', NULL, 541, 5, 'RCJ041', 'RCJ5', 741),
        (42, 'Manchester Civil Justice Centre Set 5', 0.0, 0, '1987-05-01', 'CHOF', '2007-10-01', NULL, 542, 5, 'MCJC042', 'MCJC5', 742),
        (43, 'Cardiff Crown Court Set 5', 0.0, 0, '1987-05-01', 'VENUE', '1904-01-01', NULL, 543, 5, 'CCC043', 'CCC5', 743),
        (44, 'Birmingham Civil Justice Centre Set 5', 0.0, 0, '1987-05-01','CHOY', '1987-05-01', NULL, 544, 5, 'BCJC044', 'BCJC5', 744),
        (45, 'Leeds Combined Court Centre Set 5',  0.0, 0, '1987-05-01', 'CHOA', '1993-09-15', '2025-01-01', 545, 5, 'LCCC045', 'LCCC5', 745),
        (46, 'Bristol Crown Court Set 5', 0.0, 0, '1987-05-01', 'CHOF', '1993-06-01', '2025-01-01', 546, 5, 'BCC046', 'BCC5', 746),
        (47, 'Liverpool Crown Court Set 5', 0.0, 0, '1987-05-01', 'VENUE', '1984-10-01', '2025-01-01', 547, 5, 'LCC047', 'LCC5', 747),
        (48, 'Nottingham Justice Centre Set 5',  0.0, 0, '1987-05-01', 'CHOY', '1996-04-01', '2025-01-01', 548, 5, 'NJC048', 'NJC5', 748),
        (49, 'Sheffield Combined Court Centre Set 5', 0.0, 0, '1987-05-01', 'CHOA', '1995-07-01', NULL, 549, 5, 'SCCC049', 'SCCC5', 749),
        (50, 'Newcastle Crown Court Set 5',  0.0, 0, '1987-05-01', 'CHOF', '1990-03-01', NULL, 550, 5, 'NCC050', 'NCC5', 750),
        (51, 'Royal Courts of Justice Set 6', 0.0, 0, '1987-05-01', 'VENUE', '1875-12-04', NULL, 551, 6, 'RCJ051', 'RCJ6', 751),
        (52, 'Manchester Civil Justice Centre Set 6', 0.0, 0, '1987-05-01', 'CHOY', '2007-10-01', NULL, 552, 6, 'MCJC052', 'MCJC6', 752),
        (53, 'Cardiff Crown Court Set 6', 0.0, 0, '1987-05-01', 'CHOA', '1904-01-01', '2025-01-01', 553, 6, 'CCC053', 'CCC6', 753),
        (54, 'Birmingham Civil Justice Centre Set 6', 0.0, 0, '1987-05-01','CHOF', '1987-05-01', '2025-01-01', 554, 6, 'BCJC054', 'BCJC6', 754),
        (55, 'Leeds Combined Court Centre Set 6',  0.0, 0, '1987-05-01', 'VENUE', '1993-09-15', '2025-01-01', 555, 6, 'LCCC055', 'LCCC6', 755),
        (56, 'Bristol Crown Court Set 6', 0.0, 0, '1987-05-01', 'CHOY', '1993-06-01', '2025-01-01', 556, 6, 'BCC056', 'BCC6', 756),
        (57, 'Liverpool Crown Court Set 6', 0.0, 0, '1987-05-01', 'CHOA', '1984-10-01', NULL, 557, 6, 'LCC057', 'LCC6', 757),
        (58, 'Nottingham Justice Centre Set 6',  0.0, 0, '1987-05-01', 'CHOF', '1996-04-01', NULL, 558, 6, 'NJC058', 'NJC6', 758),
        (59, 'Sheffield Combined Court Centre Set 6', 0.0, 0, '1987-05-01', 'VENUE', '1995-07-01', NULL, 559, 6, 'SCCC059', 'SCCC6', 759),
        (60, 'Newcastle Crown Court Set 6',  0.0, 0, '1987-05-01', 'CHOY', '1990-03-01', NULL, 560, 6, 'NCC060', 'NCC6', 760),
        (61, 'Royal Courts of Justice Set 7', 0.0, 0, '1987-05-01', 'CHOA', '1875-12-04', '2025-01-01', 561, 7, 'RCJ061', 'RCJ7', 761),
        (62, 'Manchester Civil Justice Centre Set 7', 0.0, 0, '1987-05-01', 'CHOF', '2007-10-01', '2025-01-01', 562, 7, 'MCJC062', 'MCJC7', 762),
        (63, 'Cardiff Crown Court Set 7', 0.0, 0, '1987-05-01', 'VENUE', '1904-01-01', '2025-01-01', 563, 7, 'CCC063', 'CCC7', 763),
        (64, 'Birmingham Civil Justice Centre Set 7', 0.0, 0, '1987-05-01','CHOY', '1987-05-01', '2025-01-01', 564, 7, 'BCJC064', 'BCJC7', 764),
        (65, 'Leeds Combined Court Centre Set 7',  0.0, 0, '1987-05-01', 'CHOA', '1993-09-15', NULL, 565, 7, 'LCCC065', 'LCCC7', 765),
        (66, 'Bristol Crown Court Set 7', 0.0, 0, '1987-05-01', 'CHOF', '1993-06-01', NULL, 566, 7, 'BCC066', 'BCC7', 766),
        (67, 'Liverpool Crown Court Set 7', 0.0, 0, '1987-05-01', 'VENUE', '1984-10-01', NULL, 567, 7, 'LCC067', 'LCC7', 767),
        (68, 'Nottingham Justice Centre Set 7',  0.0, 0, '1987-05-01', 'CHOY', '1996-04-01', NULL, 568, 7, 'NJC068', 'NJC7', 768),
        (69, 'Sheffield Combined Court Centre Set 7', 0.0, 0, '1987-05-01', 'CHOA', '1995-07-01', '2025-01-01', 569, 7, 'SCCC069', 'SCCC7', 769),
        (70, 'Newcastle Crown Court Set 7',  0.0, 0, '1987-05-01', 'CHOF', '1990-03-01', '2025-01-01', 570, 7, 'NCC070', 'NCC7', 770),
        (71, 'Royal Courts of Justice Set 8', 0.0, 0, '1987-05-01', 'VENUE', '1875-12-04', '2025-01-01', 571, 8, 'RCJ071', 'RCJ8', 771),
        (72, 'Manchester Civil Justice Centre Set 8', 0.0, 0, '1987-05-01', 'CHOY', '2007-10-01', '2025-01-01', 572, 8, 'MCJC072', 'MCJC8', 772),
        (73, 'Cardiff Crown Court Set 8', 0.0, 0, '1987-05-01', 'CHOA', '1904-01-01', NULL, 573, 8, 'CCC073', 'CCC8', 773),
        (74, 'Birmingham Civil Justice Centre Set 8', 0.0, 0, '1987-05-01','CHOF', '1987-05-01', NULL, 574, 8, 'BCJC074', 'BCJC8', 774),
        (75, 'Leeds Combined Court Centre Set 8',  0.0, 0, '1987-05-01', 'VENUE', '1993-09-15', NULL, 575, 8, 'LCCC075', 'LCCC8', 775),
        (76, 'Bristol Crown Court Set 8', 0.0, 0, '1987-05-01', 'CHOY', '1993-06-01', NULL, 576, 8, 'BCC076', 'BCC8', 776),
        (77, 'Liverpool Crown Court Set 8', 0.0, 0, '1987-05-01', 'CHOA', '1984-10-01', '2025-01-01', 577, 8, 'LCC077', 'LCC8', 777),
        (78, 'Nottingham Justice Centre Set 8',  0.0, 0, '1987-05-01', 'CHOF', '1996-04-01', '2025-01-01', 578, 8, 'NJC078', 'NJC8', 778),
        (79, 'Sheffield Combined Court Centre Set 8', 0.0, 0, '1987-05-01', 'VENUE', '1995-07-01', '2025-01-01', 579, 8, 'SCCC079', 'SCCC8', 779),
        (80, 'Newcastle Crown Court Set 8',  0.0, 0, '1987-05-01', 'CHOY', '1990-03-01', '2025-01-01', 580, 8, 'NCC080', 'NCC8', 780),
        (81, 'Royal Courts of Justice Set 9', 0.0, 0, '1987-05-01', 'CHOA', '1875-12-04', NULL, 581, 9, 'RCJ081', 'RCJ9', 781),
        (82, 'Manchester Civil Justice Centre Set 9', 0.0, 0, '1987-05-01', 'CHOF', '2007-10-01', NULL, 582, 9, 'MCJC082', 'MCJC9', 782),
        (83, 'Cardiff Crown Court Set 9', 0.0, 0, '1987-05-01', 'VENUE', '1904-01-01', NULL, 583, 9, 'CCC083', 'CCC9', 783),
        (84, 'Birmingham Civil Justice Centre Set 9', 0.0, 0, '1987-05-01','CHOY', '1987-05-01', NULL, 584, 9, 'BCJC084', 'BCJC9', 784),
        (85, 'Leeds Combined Court Centre Set 9',  0.0, 0, '1987-05-01', 'CHOA', '1993-09-15', '2025-01-01', 585, 9, 'LCCC085', 'LCCC9', 785),
        (86, 'Bristol Crown Court Set 9', 0.0, 0, '1987-05-01', 'CHOF', '1993-06-01', '2025-01-01', 586, 9, 'BCC086', 'BCC9', 786),
        (87, 'Liverpool Crown Court Set 9', 0.0, 0, '1987-05-01', 'VENUE', '1984-10-01', '2025-01-01', 587, 9, 'LCC087', 'LCC9', 787),
        (88, 'Nottingham Justice Centre Set 9',  0.0, 0, '1987-05-01', 'CHOY', '1996-04-01', '2025-01-01', 588, 9, 'NJC088', 'NJC9', 788),
        (89, 'Sheffield Combined Court Centre Set 9', 0.0, 0, '1987-05-01', 'CHOA', '1995-07-01', NULL, 589, 9, 'SCCC089', 'SCCC9', 789),
        (90, 'Newcastle Crown Court Set 9',  0.0, 0, '1987-05-01', 'CHOF', '1990-03-01', NULL, 590, 9, 'NCC090', 'NCC9', 790),
        (91, 'Royal Courts of Justice Set 10', 0.0, 0, '1987-05-01', 'VENUE', '1875-12-04', NULL, 591, 10, 'RCJ091', 'RCJ10', 791),
        (92, 'Manchester Civil Justice Centre Set 10', 0.0, 0, '1987-05-01', 'CHOY', '2007-10-01', NULL, 592, 10, 'MCJC092', 'MCJC10', 792),
        (93, 'Cardiff Crown Court Set 10', 0.0, 0, '1987-05-01', 'CHOA', '1904-01-01', '2025-01-01', 593, 10, 'CCC093', 'CCC10', 793),
        (94, 'Birmingham Civil Justice Centre Set 10', 0.0, 0, '1987-05-01','CHOF', '1987-05-01', '2025-01-01', 594, 10, 'BCJC094', 'BCJC10', 794),
        (95, 'Leeds Combined Court Centre Set 10',  0.0, 0, '1987-05-01', 'VENUE', '1993-09-15', '2025-01-01', 595, 10, 'LCCC095', 'LCCC10', 795),
        (96, 'Bristol Crown Court Set 10', 0.0, 0, '1987-05-01', 'CHOY', '1993-06-01', '2025-01-01', 596, 10, 'BCC096', 'BCC10', 796),
        (97, 'Liverpool Crown Court Set 10', 0.0, 0, '1987-05-01', 'CHOA', '1984-10-01', NULL, 597, 10, 'LCC097', 'LCC10', 797),
        (98, 'Nottingham Justice Centre Set 10',  0.0, 0, '1987-05-01', 'CHOF', '1996-04-01', NULL, 598, 10, 'NJC098', 'NJC10', 798),
        (99, 'Sheffield Combined Court Centre Set 10', 0.0, 0, '1987-05-01', 'VENUE', '1995-07-01', NULL, 599, 10, 'SCCC099', 'SCCC10', 799),
        (100, 'Newcastle Crown Court Set 10',  0.0, 0, '1987-05-01', 'CHOY', '1990-03-01', NULL, 600, 10, 'NCC100', 'NCC10', 800);

--
--
--
----------------------- APPLICATION FEE -----------------------
--
-- CO1.1 - Main Fee and Offset Fee.
INSERT INTO fee (fee_id, fee_reference, fee_description, fee_value, fee_start_date, fee_end_date, fee_version, fee_changed_by, fee_changed_date, fee_user_name) VALUES
        (nextval('fee_seq'), 'CO1.1', 'JP perform function away from court', 50.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO1.1', 'JP perform function away from court', 30.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO2.1 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO2.1', 'Application to state a case for the High Court', 515.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO2.1', 'Application to state a case for the High Court', 155.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO2.2 - Main Fee only.
        (nextval('fee_seq'), 'CO2.2', 'Appeal against DEO – Child Support Act 1991', 100.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO2.3 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO2.3', 'Appeal under Sch 5 Licensing Act 2003', 410.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO2.3', 'Appeal under Sch 5 Licensing Act 2003', 70.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO2.4 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO2.4', 'Other appeal where no fee specified', 205.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO2.4', 'Other appeal where no fee specified', 70.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO3.1 - Main Fee only.
        (nextval('fee_seq'), 'CO3.1', 'Certificate of refusal to state a case.', 105.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO3.2 - Main Fee only.
        (nextval('fee_seq'), 'CO3.2', 'Certificate of satisfaction under Register of judgments, orders and fines', 15.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO3.3 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO3.3', 'Certified copy of a memorandum of conviction.', 60.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO3.3', 'Certified copy of a memorandum of conviction.', 60.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO3.4 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO3.4', 'Certificate or certified document where no other fee is specified.', 60.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO3.4', 'Certificate or certified document where no other fee is specified.', 25.00, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO4.1 - Main Fee and Offset Fee.
        (nextval('fee_seq'), 'CO4.1', 'Liability Order – Council Tax and NDR', 325.00, DATE '2016-07-25', DATE '2018-07-24', 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        (nextval('fee_seq'), 'CO4.1', 'Liability Order – Council Tax and NDR', 0.50, DATE '2018-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload'),
        -- CO4.2 - Main Fee only.
        (nextval('fee_seq'), 'CO4.2', 'Liability Order – Child Support Act 1991', 40.00, DATE '2016-07-25', NULL, 1, -125, DATE '2025-03-25', 'AR4.Initial.SQL.Upload');
    --
    --
--

-- CRIMINAL_JUSTICE_AREA
-- Create 10 CJAs
INSERT INTO criminal_justice_area (cja_id, cja_code, cja_description) VALUES
        (1, '01', 'CJA Number 1'),
        (2, '02', 'CJA Number 2'),
        (3, '03', 'CJA Number 3'),
        (4, '04', 'CJA Number 4'),
        (5, '05', 'CJA Number 5'),
        (6, '06', 'CJA Number 6'),
        (7, '07', 'CJA Number 7'),
        (8, '08', 'CJA Number 8'),
        (9, '09', 'CJA Number 9'),
        (10, '10', 'CJA Number 10');


----------------------- APPLICATION LIST -----------------------
-- Insert 100 application_lists rows with changed_by as random UUID prefix before ':72f988bf-86f1-41af-91ab-2d7cd011db47'
INSERT INTO application_lists (
        al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse,
        list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id
)
VALUES
        (1,'OPEN', '2024-04-21', '10:00:00', 'RCJ001', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000001', 'Royal Courts of Justice Set 1', 6, 3, 1),
        (2,'OPEN', '2025-04-21', '14:00:01', 'MCJC002', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000002', 'Manchester Civil Justice Centre Set 1', 6, 3, 1),
        (3,'CLOSED', '2025-04-22', '09:00:02',  'CCC003', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000003', 'Cardiff Crown Court Set 1', 6, 3, 1),
        (4,'CLOSED', '2025-04-19', '11:30:03',  'BCJC004', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000004', 'Birmingham Civil Justice Centre Set 1',  6, 3, 1),
        (5,'OPEN', '2025-04-23', '15:00:04',  'LCCC005', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000005', 'Leeds Combined Court Centre Set 1',  6, 3, 1),
        (6,'OPEN', '2025-04-24', '13:00:05',  'BCC006', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000006', 'Bristol Crown Court Set 1', 6, 3, 1),
        (7,'OPEN', '2025-04-25', '16:00:06',  'LCC007', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000007', 'Liverpool Crown Court Set 1', 6, 3, 1),
        (8,'CLOSED', '2025-04-18', '08:30:07',  'NJC008',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000008', 'Nottingham Justice Centre Set 1', 6, 3, 1),
        (9,'CLOSED', '2025-04-17', '12:00:08',  'SCCC009', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000009', 'Sheffield Combined Court Centre Set 1', 6, 3, 1),
        (10,'OPEN', '2025-04-26', '09:30:09',  'NCC010', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000010', 'Newcastle Crown Court Set 1', 6, 3, 1),
        (11,'OPEN', '2025-04-27', '10:00:10', 'RCJ011', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000011', 'Royal Courts of Justice Set 2', 6, 3, 1),
        (12,'OPEN', '2025-04-28', '14:00:11', 'MCJC012', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000012', 'Manchester Civil Justice Centre Set 2', 6, 3, 1),
        (13,'CLOSED', '2025-04-29', '09:00:12',  'CCC013', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000013', 'Cardiff Crown Court Set 2', 6, 3, 1),
        (14,'CLOSED', '2025-04-30', '11:30:13',  'BCJC014', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000014', 'Birmingham Civil Justice Centre Set 2',  6, 3, 1),
        (15,'OPEN', '2025-05-01', '15:00:14',  'LCCC015', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000015', 'Leeds Combined Court Centre Set 2',  6, 3, 1),
        (16,'OPEN', '2025-05-02', '13:00:15',  'BCC016', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000016', 'Bristol Crown Court Set 2', 6, 3, 1),
        (17,'OPEN', '2025-05-03', '16:00:16',  'LCC017', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000017', 'Liverpool Crown Court Set 2', 6, 3, 1),
        (18,'CLOSED', '2025-05-04', '08:30:17',  'NJC018',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000018', 'Nottingham Justice Centre Set 2', 6, 3, 1),
        (19,'CLOSED', '2025-05-05', '12:00:18',  'SCCC019', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000019', 'Sheffield Combined Court Centre Set 2', 6, 3, 1),
        (20,'OPEN', '2025-05-06', '09:30:19',  'NCC020', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000020', 'Newcastle Crown Court Set 2', 6, 3, 1),
        (21,'OPEN', '2025-05-07', '10:00:20', 'RCJ021', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000021', 'Royal Courts of Justice Set 3', 6, 3, 1),
        (22,'OPEN', '2025-05-08', '14:00:21', 'MCJC022', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000022', 'Manchester Civil Justice Centre Set 3', 6, 3, 1),
        (23,'CLOSED', '2025-05-09', '09:00:22',  'CCC023', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000023', 'Cardiff Crown Court Set 3', 6, 3, 1),
        (24,'CLOSED', '2025-05-10', '11:30:23',  'BCJC024', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000024', 'Birmingham Civil Justice Centre Set 3',  6, 3, 1),
        (25,'OPEN', '2025-05-11', '15:00:24',  'LCCC025', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000025', 'Leeds Combined Court Centre Set 3',  6, 3, 1),
        (26,'OPEN', '2025-05-12', '13:00:25',  'BCC026', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000026', 'Bristol Crown Court Set 3', 6, 3, 1),
        (27,'OPEN', '2025-05-13', '16:00:26',  'LCC027', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000027', 'Liverpool Crown Court Set 3', 6, 3, 1),
        (28,'CLOSED', '2025-05-14', '08:30:27',  'NJC028',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000028', 'Nottingham Justice Centre Set 3', 6, 3, 1),
        (29,'CLOSED', '2025-05-15', '12:00:28',  'SCCC029', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000029', 'Sheffield Combined Court Centre Set 3', 6, 3, 1),
        (30,'OPEN', '2025-05-16', '09:30:29',  'NCC030', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000030', 'Newcastle Crown Court Set 3', 6, 3, 1),
        (31,'OPEN', '2025-05-17', '10:00:30', 'RCJ031', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000031', 'Royal Courts of Justice Set 4', 6, 3, 1),
        (32,'OPEN', '2025-05-18', '14:00:31', 'MCJC032', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000032', 'Manchester Civil Justice Centre Set 4', 6, 3, 1),
        (33,'CLOSED', '2025-05-19', '09:00:32',  'CCC033', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000033', 'Cardiff Crown Court Set 4', 6, 3, 1),
        (34,'CLOSED', '2025-05-20', '11:30:33',  'BCJC034', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000034', 'Birmingham Civil Justice Centre Set 4',  6, 3, 1),
        (35,'OPEN', '2025-05-21', '15:00:34',  'LCCC0035', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000035', 'Leeds Combined Court Centre Set 4',  6, 3, 1),
        (36,'OPEN', '2025-05-22', '13:00:35',  'BCC036', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000036', 'Bristol Crown Court Set 4', 6, 3, 1),
        (37,'OPEN', '2025-05-23', '16:00:36',  'LCC037', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000037', 'Liverpool Crown Court Set 4', 6, 3, 1),
        (38,'CLOSED', '2025-05-24', '08:30:37',  'NJC038',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000038', 'Nottingham Justice Centre Set 4', 6, 3, 1),
        (39,'CLOSED', '2025-05-25', '12:00:38',  'SCCC039', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000039', 'Sheffield Combined Court Centre Set 4', 6, 3, 1),
        (40,'OPEN', '2025-05-26', '09:30:39',  'NCC040', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000040', 'Newcastle Crown Court Set 4', 6, 3, 1),
        (41,'OPEN', '2025-05-27', '10:00:40', 'RCJ041', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000041', 'Royal Courts of Justice Set 5', 6, 3, 1),
        (42,'OPEN', '2025-05-28', '14:00:41', 'MCJC042', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000042', 'Manchester Civil Justice Centre Set 5', 6, 3, 1),
        (43,'CLOSED', '2025-05-29', '09:00:42',  'CCC043', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000043', 'Cardiff Crown Court Set 5', 6, 3, 1),
        (44,'CLOSED', '2025-05-30', '11:30:43',  'BCJC044', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000044', 'Birmingham Civil Justice Centre Set 5',  6, 3, 1),
        (45,'OPEN', '2025-05-31', '15:00:44',  'LCCC045', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000045', 'Leeds Combined Court Centre Set 5',  6, 3, 1),
        (46,'OPEN', '2025-06-01', '13:00:45',  'BCC046', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000046', 'Bristol Crown Court Set 5', 6, 3, 1),
        (47,'OPEN', '2025-06-02', '16:00:46',  'LCC047', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000047', 'Liverpool Crown Court Set 5', 6, 3, 1),
        (48,'CLOSED', '2025-06-03', '08:30:47',  'NJC048',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000048', 'Nottingham Justice Centre Set 5', 6, 3, 1),
        (49,'CLOSED', '2025-06-04', '12:00:48',  'SCCC049', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000049', 'Sheffield Combined Court Centre Set 5', 6, 3, 1),
        (50,'OPEN', '2025-06-05', '09:30:49',  'NCC050', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000050', 'Newcastle Crown Court Set 5', 6, 3, 1),
        (51,'OPEN', '2025-06-06', '10:00:50', 'RCJ051', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000051', 'Royal Courts of Justice Set 6', 6, 3, 1),
        (52,'OPEN', '2025-06-07', '14:00:51', 'MCJC052', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000052', 'Manchester Civil Justice Centre Set 6', 6, 3, 1),
        (53,'CLOSED', '2025-06-08', '09:00:52',  'CCC053', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000053', 'Cardiff Crown Court Set 6', 6, 3, 1),
        (54,'CLOSED', '2025-06-09', '11:30:53',  'BCJC054', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000054', 'Birmingham Civil Justice Centre Set 6',  6, 3, 1),
        (55,'OPEN', '2025-06-10', '15:00:54',  'LCCC055', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000055', 'Leeds Combined Court Centre Set 6',  6, 3, 1),
        (56,'OPEN', '2025-06-11', '13:00:55',  'BCC056', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000056', 'Bristol Crown Court Set 6', 6, 3, 1),
        (57,'OPEN', '2025-06-12', '16:00:56',  'LCC057', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000057', 'Liverpool Crown Court Set 6', 6, 3, 1),
        (58,'CLOSED', '2025-06-13', '08:30:57',  'NJC058',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000058', 'Nottingham Justice Centre Set 6', 6, 3, 1),
        (59,'CLOSED', '2025-06-14', '12:00:58',  'SCCC059', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000059', 'Sheffield Combined Court Centre Set 6', 6, 3, 1),
        (60,'OPEN', '2025-06-15', '09:30:59',  'NCC060', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000060', 'Newcastle Crown Court Set 6', 6, 3, 1),
        (61,'OPEN', '2025-06-16', '10:00:00', 'RCJ061', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000061', 'Royal Courts of Justice Set 7', 6, 3, 1),
        (62,'OPEN', '2025-06-17', '14:00:01', 'MCJC062', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000062', 'Manchester Civil Justice Centre Set 7', 6, 3, 1),
        (63,'CLOSED', '2025-06-18', '09:00:02',  'CCC063', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000063', 'Cardiff Crown Court Set 7', 6, 3, 1),
        (64,'CLOSED', '2025-06-19', '11:30:03',  'BCJC064', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000064', 'Birmingham Civil Justice Centre Set 7',  6, 3, 1),
        (65,'OPEN', '2025-06-20', '15:00:04',  'LCCC065', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000065', 'Leeds Combined Court Centre Set 7',  6, 3, 1),
        (66,'OPEN', '2025-06-21', '13:00:05',  'BCC066', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000066', 'Bristol Crown Court Set 7', 6, 3, 1),
        (67,'OPEN', '2025-06-22', '16:00:06',  'LCC067', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000067', 'Liverpool Crown Court Set 7', 6, 3, 1),
        (68,'CLOSED', '2025-06-23', '08:30:07',  'NJC068',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000068', 'Nottingham Justice Centre Set 7', 6, 3, 1),
        (69,'CLOSED', '2025-06-24', '12:00:08',  'SCCC069', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000069', 'Sheffield Combined Court Centre Set 7', 6, 3, 1),
        (70,'OPEN', '2025-06-25', '09:30:09',  'NCC070', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000070', 'Newcastle Crown Court Set 7', 6, 3, 1),
        (71,'OPEN', '2025-06-26', '10:00:10', 'RCJ071', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000071', 'Royal Courts of Justice Set 8', 6, 3, 1),
        (72,'OPEN', '2025-06-27', '14:00:11', 'MCJC072', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000072', 'Manchester Civil Justice Centre Set 8', 6, 3, 1),
        (73,'CLOSED', '2025-06-28', '09:00:12',  'CCC073', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000073', 'Cardiff Crown Court Set 8', 6, 3, 1),
        (74,'CLOSED', '2025-06-29', '11:30:13',  'BCJC074', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000074', 'Birmingham Civil Justice Centre Set 8',  6, 3, 1),
        (75,'OPEN', '2025-06-30', '15:00:14',  'LCCC075', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000075', 'Leeds Combined Court Centre Set 8',  6, 3, 1),
        (76,'OPEN', '2025-07-01', '13:00:15',  'BCC076', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000076', 'Bristol Crown Court Set 8', 6, 3, 1),
        (77,'OPEN', '2025-07-02', '16:00:16',  'LCC077', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000077', 'Liverpool Crown Court Set 8', 6, 3, 1),
        (78,'CLOSED', '2025-07-03', '08:30:17',  'NJC078',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000078', 'Nottingham Justice Centre Set 8', 6, 3, 1),
        (79,'CLOSED', '2025-07-04', '12:00:18',  'SCCC079', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000079', 'Sheffield Combined Court Centre Set 8', 6, 3, 1),
        (80,'OPEN', '2025-07-05', '09:30:19',  'NCC080', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000080', 'Newcastle Crown Court Set 8', 6, 3, 1),
        (81,'OPEN', '2025-07-06', '10:00:20', 'RCJ081', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000081', 'Royal Courts of Justice Set 9', 6, 3, 1),
        (82,'OPEN', '2025-07-07', '14:00:21', 'MCJC082', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000082', 'Manchester Civil Justice Centre Set 9', 6, 3, 1),
        (83,'CLOSED', '2025-07-08', '09:00:22',  'CCC083', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000083', 'Cardiff Crown Court Set 9', 6, 3, 1),
        (84,'CLOSED', '2025-07-09', '11:30:23',  'BCJC084', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000084', 'Birmingham Civil Justice Centre Set 9',  6, 3, 1),
        (85,'OPEN', '2025-07-10', '15:00:24',  'LCCC085', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000085', 'Leeds Combined Court Centre Set 9',  6, 3, 1),
        (86,'OPEN', '2025-07-11', '13:00:25',  'BCC086', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000086', 'Bristol Crown Court Set 9', 6, 3, 1),
        (87,'OPEN', '2025-07-12', '16:00:26',  'LCC087', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000087', 'Liverpool Crown Court Set 9', 6, 3, 1),
        (88,'CLOSED', '2025-07-13', '08:30:27',  'NJC088',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000088', 'Nottingham Justice Centre Set 9', 6, 3, 1),
        (89,'CLOSED', '2025-07-14', '12:00:28',  'SCCC089', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000089', 'Sheffield Combined Court Centre Set 9', 6, 3, 1),
        (90,'OPEN', '2025-07-15', '09:30:29',  'NCC090', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000090', 'Newcastle Crown Court Set 9', 6, 3, 1),
        (91,'OPEN', '2025-07-16', '10:00:30', 'RCJ091', null, 'Morning list for Family Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000091', 'Royal Courts of Justice Set 10', 6, 3, 1),
        (92,'OPEN', '2025-07-17', '14:00:31', 'MCJC092', null, 'Afternoon list for Civil Court', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000092', 'Manchester Civil Justice Centre Set 10', 6, 3, 1),
        (93,'CLOSED', '2025-07-18', '09:00:32',  'CCC093', null ,'Cancelled hearing for Probate', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000093', 'Cardiff Crown Court Set 10', 6, 3, 1),
        (94,'CLOSED', '2025-07-19', '11:30:33',  'BCJC094', null, 'Completed list - criminal matters', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000094', 'Birmingham Civil Justice Centre Set 10',  6, 3, 1),
        (95,'OPEN', '2025-07-20', '15:00:34',  'LCCC095', null, 'Immigration list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000095', 'Leeds Combined Court Centre Set 10',  6, 3, 1),
        (96,'OPEN', '2025-07-21', '13:00:35',  'BCC096', null, 'Tribunal cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000096', 'Bristol Crown Court Set 10', 6, 3, 1),
        (97,'OPEN', '2025-07-22', '16:00:36',  'LCC097', null, 'Urgent list', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000097', 'Liverpool Crown Court Set 10', 6, 3, 1),
        (98,'CLOSED', '2025-07-23', '08:30:37',  'NJC098',null,'No show', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000098', 'Nottingham Justice Centre Set 10', 6, 3, 1),
        (99,'CLOSED', '2025-07-24', '12:00:38',  'SCCC099', null, 'Family cases', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000099', 'Sheffield Combined Court Centre Set 10', 6, 3, 1),
        (100,'OPEN', '2025-07-25', '09:30:39',  'NCC100', null, 'New applications', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_DATE, '12345678-aaaa-bbbb-cccc-000000000100', 'Newcastle Crown Court Set 10', 6, 3, 1)
;
--
-- ----------------------- APPLICATION CODES -----------------------
--
INSERT INTO application_codes (ac_id, application_code, application_code_title, application_code_wording, application_legislation,fee_due, application_code_respondent, ac_destination_email_address_1, ac_destination_email_address_2, application_code_start_date, application_code_end_date, bulk_respondent_allowed, version, changed_by, changed_date, user_name, ac_fee_reference)
VALUES
        (1, 'AD99001','Copy documents','Request to copy documents','',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', 'CO1.1'),
        (2, 'AD99002','Copy documents (electronic)','Request for copy documents on computer disc or in electronic form','',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00', 'admin', 'CO1.1'),
        (3, 'AD99003','Extract from the Court Register','Certified extract from the court register','',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin','CO1.1'),
        (4, 'AD99004','Certificate of Satisfaction','Request for a certificate of satisfaction of debt registered in the register of judgements, orders and fines','',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (5, 'AD99005','Certified genuine copy document','Request for a copy of a document certified as a genuine copy of the original document','',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (6, 'AP99001','Appeal to Crown Court','Notice of appeal in respect of a case heard on {TEXT|Date of Hearing|10}','Section 108 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,'2016-01-01 00:00:00','admin',NULL),
        (7, 'AP99002','Appeal by Case Stated (Crime)','Notice of appeal to the High Court by way of case stated in respect of a criminal case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (8, 'AP99003','Appeal by Case Stated (Civil)','Notice of appeal to the High Court by way of case stated in respect of case heard on {TEXT|Date of Hearing|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (9, 'AP99004','Request for Certificate of Refusal to State a Case (Civil)','Request for a certificate of refusal to state a case for the opinion of the High Court in respect of civil proceedings heard on {TEXT|Date|10}','Section 111 Magistrates'' Courts Act 1980',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (10, 'CT99001','Issue of liability order summons -council tax (bulk)','Attends to swear a complaint for the issue summonses for the debtors to answer an application for a liability order in relation to unpaid council tax (number of cases {TEXT|Number|4})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,0,'','',DATE '2016-01-01',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (11, 'CT99002','Issue of liability order summons - council tax','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid council tax (reference {TEXT|Reference|100})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (12, 'CT99003','Issue of liability order summons -non-domestic rate (bulk)','Attends to swear a complaint for the issue summonses for the debtors to answer an application for a liability order in relation to unpaid non-domestic rate (number of cases {TEXT|Number|4})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,0,'','',DATE '2016-01-01',NULL,1,0, 0, '2016-01-01 00:00:00','admin',NULL),
        (13, 'CT99004','Issue of liability order summons - non-domestic rate','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid non-domestic rate (reference {TEXT|Reference|100})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (14, 'CT99005','Issue of warrant of arrest in commitment proceedings - council tax (bulk)','Attends to swear a complaint for the issue of warrants of arrest for the debtors to answer an application for committal to prison (number of cases {TEXT|Number|4})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,0,'','',DATE '2016-01-01',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (15, 'CT99006','Issue of warrant of arrest in commitment proceedings - council tax','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for committal to prison in relation to unpaid council tax (reference {TEXT|Reference|100})','Regulation 34 Council Tax (Admin and Enforcement) Regulations 1992',0,1,'','',DATE '2016-01-01',NULL,0,0, 0, '2016-01-01 00:00:00','admin',NULL),
        (16, 'CT99007','Issue of warrant of arrest in commitment proceedings - non-domestic rate (bulk)','Attends to swear a complaint for the issue of warrants of arrest for the debtors to answer an application for commitment to prison in relation to unpaid non-domestic rate (number of cases {TEXT|Number|4})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,0,'','',DATE '2016-01-01',NULL,1, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (17, 'CT99008','Issue of warrant of arrest in commitment proceedings - non-domestic rate','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for commitment to prison in relation to unpaid non-domestic rate (reference {TEXT|Reference|100})','Regulation 12 Non-Domestic Rating (Collection etc.) Regulations 1989',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (18, 'CT99009','Issue of liability order summons - child support','Attends to swear a complaint for the issue of a summons for the debtor to answer an application for a liability order in relation to unpaid child support (reference {TEXT|Reference|100})','Regulation 28 of the Child Support (Collection and Enforcement) Regulations 1992',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0,'2016-01-01 00:00:00','admin',NULL),
        (19, 'CT99010','Issue of liability order warrant in commitment proceedings - child support','Attends to swear a complaint for the issue of an arrest warrant for the debtor to answer an application for committal to prison in relation to unpaid child support (reference {TEXT|Reference|100})','Regulation 28 of the Child Support (Collection and Enforcement) Regulations 1992',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (20, 'MS99001','Special Constable''s Attestation','Attends to swear oath of the office of Special Constable','Section 29 Police Act 1996',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
        (21, 'MS99002','Change of name','Attends to make a statutory declaration that henceforth the applicant will be known as {TEXT|New Name|100}','Section 18 Statutory Declarations Act 1835',0,0,'','',DATE '2016-01-01',NULL,0,0, 0, '2016-01-01 00:00:00', 'admin', NULL),
        (22, 'MS99003','Statutory Decalration - Local Authority Car Park','Attends to make a statutory declaration in relation to car park penalty issued on {TEXT|Date|10} for vehicle {TEXT|Vehicle Reg|10}','Section 18 Statutory Declarations Act 1835',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (23, 'MS99004','Statutory Decalration -Lost documents','Attends to make a statutory declaration in relation to loss of original document, namely {TEXT|Specify Document Lost|100}','Section 18 Statutory Declarations Act 1835',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (24, 'RE99001','Rights of Entry Warrant - Gas Operator','Application by a gas operator for a warrant to enter premises at {TEXT|Premises Address|200}','Section 2 Rights of Entry (Gas and Electricity Boards) Act 1954',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (25, 'RE99002','Rights of Entry Warrant - Electricity Operator','Application by an electricity operator for a warrant to enter premises at {TEXT|Premises Address|200}','Section 2 Rights of Entry (Gas and Electricity Boards) Act 1954',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
        (26, 'RE99003','Rights of Entry Warrant - Food Safety Regs','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Reg 16 Food Safety and Hygiene (England) Regulations 2013',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (27, 'RE99004','Rights of Entry Warrant - Food Safety Act 1990','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Section 32 of the Food Safety Act 1990',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (28, 'RE99005','Rights of Entry Warrant - dairy premises','Application for a warrant to enter premises at {TEXT|Premises Address|200}','Reg 9 Single Common Market Organisation (Emergency Aid for Milk Producers) Regulations 2015',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (29, 'SW99001','Search Warrant - Stolen Goods','Application for a search warrant in respect of stolen goods under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (30, 'SW99002','Search Warrant - Controlled Drugs','Application for a search warrant in respect of controlled drugs reference under number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',DATE '2016-01-01',NULL,0,0, 0, '2016-01-01 00:00:00', 'admin',NULL),
        (31, 'SW99003','Search Warrant - Psychoactive Substance','Application for a search warrant in respect of psychoactive substances under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (32, 'SW99004','Search Warrant - Firearms','Application for a search warrant in respect of firearms under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin',NULL),
        (33, 'SW99005','Search Warrant - Evidential Material','Application for a search warrant in respect of evidential material under reference number {TEXT|Reference|100}','Section 8 Police and Criminal Evidence Act 1984',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
        (34, 'SW99006','Search Warrant - Mental Health','Application for a warrant to search for a person and if needs be remove to a place of safety.','Section 135 of the Mental Health Act 1983',1,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin',NULL),
        (35, 'SW99007','Inspection of Bankers'' Books (criminal proceedings)','Application for an order to allow the applicant to inspect or take copies of bankers books held by {TEXT|Name of Bank|100} in respect of criminal proceedings at {TEXT|Name of Court|100}.','Section 7 Bankers''' || ' Books Evidence Act 1879',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
        (36, 'SW99008','Inspection of Bankers'' Books (civil or other proceedings)','Application for an order to allow the applicant to inspect or take copies of bankers books held by {TEXT|Name of Bank|100} in respect of civil or other proceedings at {TEXT|Name of Court|100}.','Section 7 Bankers''' || ' Books Evidence Act 1879',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00','admin', NULL),
        (37, 'SW99009','Approval of Access to Communications Data','Approval of an authorisation issued by the applicant for access to communications data reference {TEXT|Reference|100}','Section 23A Regulation of Investigatory Powers Act 2000',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
        (38, 'SW99010','Approval of Use of Directed Surveillance','Approval of an authorisation issued by the applicant for the use of directed surveillance reference {TEXT|Reference|100}','Sections 28 and 32A Regulation of Investigatory Powers Act 2000',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
        (39, 'SW99011','Approval of Use of Covert Human Intelligence Source','Approval of an authorisation issued by the applicant for the use of covert human intelligence sources reference {TEXT|Reference|100}','Sections 29 and 32A Regulation of Investigatory Powers Act 2000',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0,  '2016-01-01 00:00:00','admin', NULL),
        (40, 'MS99005','Statutory Decalration - Non-standard Civil','Attends to make a statutory declaration in relation to  {TEXT|Specify Nature of Declaration|200}','Section 18 Statutory Declarations Act 1835',0,0,'','',DATE '2016-01-01',NULL,0, 0, 0, '2016-01-01 00:00:00', 'admin', NULL),
        (41, 'MS99006','Condemnation of Unfit Food','Application for the condemnation of food, namely  {TEXT|Describe Seized Food|100}','Section 9 Food Safety Act 1990',0,1,'','',DATE '2016-01-01',NULL,0, 0, 0,'2016-01-01 00:00:00', 'admin', NULL);
--
--
--
------------------------- IDENTITY_DETAILS -----------------------
--
INSERT INTO name_address (na_id, code, name, title, forename_1, forename_2, forename_3, surname, address_l1, address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number, version, changed_by, changed_date, user_name, date_of_birth, dms_id)
VALUES
        (1, 'RE', 'Jack Turner', 'Mr', 'John', NULL, NULL, 'Turner', '1 Market Street', NULL, NULL, NULL, NULL, 'AB11 2CD', 'john.smith@example.com', '01234567890', NULL,  0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', TIMESTAMP '2016-01-01 00:00:00' , NULL, NULL, NULL),
        (2, 'RE', 'Sarah Johnson', 'Mrs', 'Sarah', NULL, NULL, 'Johnson', '12 The Avenue', NULL, NULL, NULL, NULL, 'XY9 8ZZ', 's.johnson@example.com', NULL, '07700900000',  0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', TIMESTAMP' 2016-01-01 00:00:00' , NULL, NULL, NULL),
        (3, 'RE', 'Sam Burton', NULL, 'Sam', NULL, NULL, 'Burton', 'Flat 4, 22 Hillside', NULL, NULL, NULL, NULL, 'SN12 1ZZ', NULL, NULL, NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', '2016-01-01 00:00:00' , NULL, NULL, NULL),
        (4, 'AP', 'Legal Aid Board', NULL, NULL, NULL, NULL, NULL, '100 Legal Street', NULL, NULL, NULL, NULL, 'BA15 1LA', 'info@legalaid.example.com', NULL, NULL, 0, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', TIMESTAMP '2016-01-01 00:00:00', NULL, NULL, NULL);
--
--
--
-- ----------------------- STANDARD_APPLICANT -----------------------
--
INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date,
                                 version,  changed_by, changed_date, user_name, name, title, forename_1, forename_2, forename_3, surname, address_l1,
                                 address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number) VALUES
        (1, 'APP001', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000001', 'John Smith', 'Mr', 'John', NULL, NULL, 'Smith','123 High Street', NULL, NULL, 'Townsville', NULL,'TS1 1AB', 'john.smith@example.com', '01234567890', '07123456789'),
        (2, 'APP002', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000002',  'Jane Doe', 'Ms', 'Jane', NULL, NULL, 'Doe','456 Elm Road', 'Apt 5', NULL, 'Cityville', NULL,'CV2 2BC', 'jane.doe@example.com', '02345678901', NULL),
        (3, 'APP003',  CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000003',  'Alex Dunn', 'Dr', 'Alex', 'Taylor', NULL, 'Dunn','789 Oak Avenue', NULL, NULL, 'Villageham', 'Countyshire','VH3 3CD', 'alex.johnson@example.com', NULL, '07987654321'),
        (4, 'APP004', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000004', 'Sam Burton', 'Mr', 'Sam', NULL, NULL, 'Burton','789 Pine Lane', NULL, NULL, 'Hamlet', NULL,'HM4 4EF', 'sam.burton@example.com', '03456789012', NULL),
        (5, 'APP005', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000005', 'Emily Carter', 'Mrs', 'Emily', NULL, NULL, 'Carter','321 Willow Way', NULL, NULL, 'Villageton', NULL,'VT5 5GH', 'emily.carter@example.com', '04567890123', '07234567890'),
        (6, 'APP006', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000006', 'Michael Evans', 'Mr', 'Michael', NULL, NULL, 'Evans','654 Cedar Road', NULL, NULL, 'Townham', NULL,'TH6 6IJ', 'michael.evans@example.com', '05678901234', NULL),
        (7, 'APP007', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000007', 'Olivia Harris', 'Ms', 'Olivia', NULL, NULL, 'Harris','987 Birch Street', NULL, NULL, 'Cityton', NULL,'CT7 7KL', 'olivia.harris@example.com', '06789012345', '07345678901'),
        (8, 'APP008', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000008', 'William Scott', 'Mr', 'William', NULL, NULL, 'Scott','159 Spruce Avenue', NULL, NULL, 'Hamletville', NULL,'HV8 8MN', 'william.scott@example.com', '07890123456', NULL),
        (9, 'APP009', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000009', 'Sophia King', 'Miss', 'Sophia', NULL, NULL, 'King','753 Maple Crescent', NULL, NULL, 'Villageshire', NULL,'VS9 9OP', 'sophia.king@example.com', '08901234567', '07456789012'),
        (10, 'APP010', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000010', 'James Lee', 'Mr', 'James', NULL, NULL, 'Lee','852 Oak Lane', NULL, NULL, 'Hamletford', NULL,'HF10 0QR', 'james.lee@example.com', '09012345678', NULL),
        (11, 'APP011', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000011', 'Charlotte Walker', 'Mrs', 'Charlotte', NULL, NULL, 'Walker','951 Elm Street', NULL, NULL, 'Townborough', NULL,'TB11 1ST', 'charlotte.walker@example.com', '01234567891', '07567890123'),
        (12, 'APP012', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000012', 'Benjamin Young', 'Mr', 'Benjamin', NULL, NULL, 'Young','357 Ash Road', NULL, NULL, 'Cityshire', NULL,'CS12 2UV', 'benjamin.young@example.com', '02345678912', NULL),
        (13, 'APP013', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000013', 'Amelia Hall', 'Ms', 'Amelia', NULL, NULL, 'Hall','258 Fir Avenue', NULL, NULL, 'Villagefield', NULL,'VF13 3WX', 'amelia.hall@example.com', '03456789123', '07678901234'),
        (14, 'APP014', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000014', 'Daniel Wright', 'Mr', 'Daniel', NULL, NULL, 'Wright','654 Poplar Lane', NULL, NULL, 'Hamletton', NULL,'HT14 4YZ', 'daniel.wright@example.com', '04567891234', NULL),
        (15, 'APP015', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000015', 'Mia Green', 'Miss', 'Mia', NULL, NULL, 'Green','753 Willow Street', NULL, NULL, 'Townfield', NULL,'TF15 5AB', 'mia.green@example.com', '05678912345', '07789012345'),
        (16, 'APP016', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000016', 'Henry Adams', 'Mr', 'Henry', NULL, NULL, 'Adams','852 Cedar Crescent', NULL, NULL, 'Cityford', NULL,'CF16 6CD', 'henry.adams@example.com', '06789123456', NULL),
        (17, 'APP017', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000017', 'Isabella Baker', 'Mrs', 'Isabella', NULL, NULL, 'Baker','951 Pine Avenue', NULL, NULL, 'Villageborough', NULL,'VB17 7EF', 'isabella.baker@example.com', '07891234567', '07890123456'),
        (18, 'APP018', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000018', 'Jack Carter', 'Mr', 'Jack', NULL, NULL, 'Carter','357 Maple Road', NULL, NULL, 'Hamletshire', NULL,'HS18 8GH', 'jack.carter@example.com', '08912345678', NULL),
        (19, 'APP019', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000019', 'Emily Davis', 'Ms', 'Emily', NULL, NULL, 'Davis','258 Oak Lane', NULL, NULL, 'Townham', NULL,'TH19 9IJ', 'emily.davis@example.com', '09012345679', '07901234567'),
        (20, 'APP020', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000020', 'Thomas Evans', 'Mr', 'Thomas', NULL, NULL, 'Evans','654 Elm Avenue', NULL, NULL, 'Cityton', NULL,'CT20 0KL', 'thomas.evans@example.com', '01234567892', NULL),
        (21, 'APP021', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000021', 'Grace Foster', 'Miss', 'Grace', NULL, NULL, 'Foster','753 Spruce Street', NULL, NULL, 'Villagefield', NULL,'VF21 1MN', 'grace.foster@example.com', '02345678913', '07012345678'),
        (22, 'APP022', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000022', 'Matthew Graham', 'Mr', 'Matthew', NULL, NULL, 'Graham','852 Fir Crescent', NULL, NULL, 'Hamletville', NULL,'HV22 2OP', 'matthew.graham@example.com', '03456789124', NULL),
        (23, 'APP023', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000023', 'Ella Harris', 'Ms', 'Ella', NULL, NULL, 'Harris','951 Poplar Avenue', NULL, NULL, 'Townborough', NULL,'TB23 3QR', 'ella.harris@example.com', '04567891235', '07123456789'),
        (24, 'APP024', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000024', 'George Hill', 'Mr', 'George', NULL, NULL, 'Hill','357 Willow Road', NULL, NULL, 'Cityshire', NULL,'CS24 4ST', 'george.hill@example.com', '05678912346', NULL),
        (25, 'APP025', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000025', 'Ava Johnson', 'Miss', 'Ava', NULL, NULL, 'Johnson','258 Cedar Lane', NULL, NULL, 'Villageham', NULL,'VH25 5UV', 'ava.johnson@example.com', '06789123457', '07234567891'),
        (26, 'APP026', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000026', 'Samuel Kelly', 'Mr', 'Samuel', NULL, NULL, 'Kelly','654 Pine Crescent', NULL, NULL, 'Hamletton', NULL,'HT26 6WX', 'samuel.kelly@example.com', '07891234568', NULL),
        (27, 'APP027', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000027', 'Chloe Lewis', 'Ms', 'Chloe', NULL, NULL, 'Lewis','951 Maple Avenue', NULL, NULL, 'Townfield', NULL,'TF27 7YZ', 'chloe.lewis@example.com', '08912345679', '07345678902'),
        (28, 'APP028', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000028', 'Jacob Martin', 'Mr', 'Jacob', NULL, NULL, 'Martin','357 Oak Street', NULL, NULL, 'Cityford', NULL,'CF28 8AB', 'jacob.martin@example.com', '09012345680', NULL),
        (29, 'APP029', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000029', 'Lily Nelson', 'Miss', 'Lily', NULL, NULL, 'Nelson','258 Elm Lane', NULL, NULL, 'Villageborough', NULL,'VB29 9CD', 'lily.nelson@example.com', '01234567893', '07456789013'),
        (30, 'APP030', CURRENT_DATE, NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000030', 'David Owen', 'Mr', 'David', NULL, NULL, 'Owen','654 Spruce Avenue', NULL, NULL, 'Hamletshire', NULL,'HS30 0EF', 'david.owen@example.com', '02345678914', NULL);
--
--
--
-- ----------------------- APPLICATIONS -----------------------
/*INSERT INTO application_list_entries (ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents, application_list_entry_wording, case_reference, entry_rescheduled, notes, version, changed_by, changed_date, bulk_upload, sequence_number, lodgement_date) VALUES
        (1, 1, 1, 1, NULL,NULL, 1, 'Request to copy documents', 'CASE123456', 'N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', CURRENT_TIMESTAMP),
        (2, 1, 2, 2, NULL, 2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs',1, 1,CURRENT_TIMESTAMP, 'Y', '1', CURRENT_TIMESTAMP),
        (3, 1, 2, 2, NULL,4, 0,'Court register extract application', 'CASE123458', 'N', NULL, 1, 1,CURRENT_TIMESTAMP, 'N', 1, CURRENT_TIMESTAMP),
        (4, 1, 1, 1, NULL,1,0,'Application to copy documents (Standard)', 'CASE123456','N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', CURRENT_TIMESTAMP),
        (5, 2, 2, 2, NULL,2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs', 1, 1,CURRENT_TIMESTAMP, 'Y', '1', CURRENT_TIMESTAMP),
        (6, 2, 2, 2, NULL, 3, 4, 'Court register extract application', 'CASE123458','N', NULL, 1, 1,CURRENT_TIMESTAMP, 'N', 1, CURRENT_TIMESTAMP),
        (7, 2, 1, 1, NULL,1,0,'Application to copy documents (Standard)', 'CASE123456', 'N', 'Standard application with no respondent', 1, 1,CURRENT_TIMESTAMP, 'N', '1', CURRENT_TIMESTAMP),
        (8, 2, 2, 2, NULL,2, 0,'Electronic document request with 3 respondents', 'CASE123457', 'Y', 'Rescheduled due to missing docs', 1, 1,CURRENT_TIMESTAMP, 'Y', '1', CURRENT_TIMESTAMP),
        (9, 2, 2, 2, NULL,4, 0,'Court register extract application', 'CASE123458', 'N', NULL, 1, 1,CURRENT_TIMESTAMP,'N', '1', CURRENT_TIMESTAMP);
*/

        -- Generate all permutations of application_list_entries for every combination of application_lists, standard_applicants, and application_codes
        INSERT INTO application_list_entries (
                ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents,
                application_list_entry_wording, case_reference, entry_rescheduled, notes, version,
                changed_by, changed_date, bulk_upload, sequence_number, lodgement_date
        )
        SELECT
                ROW_NUMBER() OVER (ORDER BY al.al_id, sa.sa_id, ac.ac_id) + (
                        SELECT COALESCE(MAX(ale_id), 0) FROM application_list_entries
                ) AS ale_id,
                al.al_id AS al_al_id,
                sa.sa_id AS sa_sa_id,
                ac.ac_id AS ac_ac_id,
                NULL AS a_na_id,
                NULL AS r_na_id,
                0 AS number_of_bulk_respondents,
                ac.application_code_title AS application_list_entry_wording,
                CONCAT('CASE', LPAD(al.al_id::text, 3, '0'), '-', LPAD(sa.sa_id::text, 3, '0'), '-', LPAD(ac.ac_id::text, 3, '0')) AS case_reference,
                'N' AS entry_rescheduled,
                NULL AS notes,
                1 AS version,
                gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47' AS changed_by,
                CURRENT_TIMESTAMP AS changed_date,
                'N' AS bulk_upload,
                1 AS sequence_number,
                CURRENT_TIMESTAMP AS lodgement_date
        FROM
                application_lists al
        CROSS JOIN
                standard_applicants sa
        CROSS JOIN
                application_codes ac
        WHERE NOT EXISTS (
                SELECT 1 FROM application_list_entries ale
                WHERE ale.al_al_id = al.al_id
                  AND ale.sa_sa_id = sa.sa_id
                  AND ale.ac_ac_id = ac.ac_id
        );

-- APP_LIST_ENTRY_FEE_ID
-- Populate app_list_entry_fee_id: For each application_list_entry, if the related application_code has a non-null ac_fee_reference, link to the latest fee_id for that reference
INSERT INTO app_list_entry_fee_id (
        ale_ale_id,
        fee_fee_id,
        version,
        changed_by,
        changed_date,
        user_name
)
SELECT
        ale.ale_id,
        f.fee_id,
        1 AS version,
        gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47' AS changed_by,
        CURRENT_TIMESTAMP AS changed_date,
        'AR4.Initial.SQL.Upload' AS user_name
FROM application_list_entries ale
JOIN application_codes ac ON ale.ac_ac_id = ac.ac_id
JOIN LATERAL (
        SELECT fee_id
        FROM fee
        WHERE fee_reference = ac.ac_fee_reference
        ORDER BY fee_start_date DESC
        LIMIT 1
) f ON ac.ac_fee_reference IS NOT NULL;


--
--
--
-- ----------------------- APPLICATION_FEE_RECORD -----------------------
/*INSERT INTO app_list_entry_fee_status (alefs_id, alefs_ale_id, alefs_payment_reference, alefs_fee_status, alefs_fee_status_date,  alefs_version, alefs_changed_by, alefs_changed_date, alefs_user_name, alefs_status_creation_date) VALUES
        (1, 1, 'TESTPAY123', 'D', DATE '2025-05-15', 1, 1,  DATE '2025-05-15', 'AR4.Initial.SQL.Upload', DATE '2025-05-15');*/


        -- Sample payments for some application_list_entries (not 1-1, just a few random ones)
        INSERT INTO app_list_entry_fee_status (
                alefs_id, alefs_ale_id, alefs_payment_reference, alefs_fee_status, alefs_fee_status_date,
                alefs_version, alefs_changed_by, alefs_changed_date, alefs_user_name, alefs_status_creation_date
        ) VALUES
                (2, 5, 'PAYREF1001', 'P', DATE '2025-05-16', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-16', 'AR4.Initial.SQL.Upload', DATE '2025-05-16'),
                (3, 10, 'PAYREF1002', 'D', DATE '2025-05-17', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-17', 'AR4.Initial.SQL.Upload', DATE '2025-05-17'),
                (4, 15, 'PAYREF1003', 'P', DATE '2025-05-18', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-18', 'AR4.Initial.SQL.Upload', DATE '2025-05-18'),
                (5, 20, 'PAYREF1004', 'D', DATE '2025-05-19', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-19', 'AR4.Initial.SQL.Upload', DATE '2025-05-19'),
                (6, 25, 'PAYREF1005', 'P', DATE '2025-05-20', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-20', 'AR4.Initial.SQL.Upload', DATE '2025-05-20'),
                (7, 50, 'PAYREF1006', 'D', DATE '2025-05-21', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-21', 'AR4.Initial.SQL.Upload', DATE '2025-05-21'),
                (8, 75, 'PAYREF1007', 'P', DATE '2025-05-22', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-22', 'AR4.Initial.SQL.Upload', DATE '2025-05-22'),
                (9, 100, 'PAYREF1008', 'D', DATE '2025-05-23', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', DATE '2025-05-23', 'AR4.Initial.SQL.Upload', DATE '2025-05-23');
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
        (1,1,1,'Application granted in full.','Magistrate Jane Doe',1,gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, NULL),
        (2,2,1, 'Refused due to lack of supporting documents.','Magistrate John Smith',1,gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',CURRENT_TIMESTAMP, NULL);
        -- Insert more random application results for demonstration

-- And some more random ones
INSERT INTO app_list_entry_resolutions (aler_id, rc_rc_id, ale_ale_id, al_entry_resolution_wording, al_entry_resolution_officer, version, changed_by, changed_date, user_name) VALUES
                (3, 3, 2, 'Case stated for High Court opinion.', 'Magistrate Alice Brown', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (4, 4, 3, 'Collection order made for outstanding fees.', 'Magistrate Bob Green', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (5, 5, 4, 'Fee remitted due to benefits.', 'Magistrate Carol White', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (6, 6, 5, 'Fee remitted for other reasons.', 'Magistrate David Black', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (7, 9, 6, 'Application refused.', 'Magistrate Emma Grey', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (8, 10, 7, 'Respondent attended the hearing.', 'Magistrate Frank Blue', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (9, 12, 8, 'Referred for full court hearing.', 'Magistrate Grace Red', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (10, 13, 9, 'Statutory declaration accepted.', 'Magistrate Henry Violet', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (11, 14, 10, 'Summons issued.', 'Magistrate Ivy Orange', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL),
                (12, 18, 11, 'Application withdrawn.', 'Magistrate Jack Indigo', 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, NULL);

-- COMMUNICATION_MEDIA
-- Populate communication_media with some rows
INSERT INTO communication_media (comm_id, detail, start_date, end_date, version_number, changed_by, changed_date) VALUES
        (1, 'Email', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP),
        (2, 'Business Telephone', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP),
        (3, 'Fax', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP),
        (4, 'Direct Dial', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP),
        (5, 'Mobile Telephone', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP);

-- LINK_COMMUNICATION_MEDIA
-- Populate link_communication_media with some rows referencing national_court_houses.loc_loc_id and some unrelated rows

-- Assume communication_media table has at least comm_id 1-5, and bu_bu_id and er_er_id can be arbitrary for unrelated rows
-- Ensure at least one 'BT' (Business Telephone) link_communication_media for each 'CHOA' court house with non-null end_date

-- Insert the required rows
INSERT INTO link_communication_media (
        lcm_id, lcm_type, start_date, end_date, version_number, changed_by, changed_date, comm_comm_id, loc_loc_id, er_er_id, bu_bu_id
        )
        VALUES
                -- Rows referencing national_court_houses.loc_loc_id (use first 25 court houses as example)
                (1, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 501, NULL, NULL),
                (2, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 502, NULL, NULL),
                (3, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 503, NULL, NULL),
                (4, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 504, NULL, NULL),
                (5, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 505, NULL, NULL),
                (6, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 506, NULL, NULL),
                (7, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 507, NULL, NULL),
                (8, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 508, NULL, NULL),
                (9, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 509, NULL, NULL),
                (10, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 510, NULL, NULL),
                (11, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 511, NULL, NULL),
                (12, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 512, NULL, NULL),
                (13, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 513, NULL, NULL),
                (14, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 514, NULL, NULL),
                (15, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 515, NULL, NULL),
                (16, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 516, NULL, NULL),
                (17, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 517, NULL, NULL),
                (18, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 518, NULL, NULL),
                (19, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 519, NULL, NULL),
                (20, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 520, NULL, NULL),
                (21, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 521, NULL, NULL),
                (22, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 522, NULL, NULL),
                (23, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 523, NULL, NULL),
                (24, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 524, NULL, NULL),
                (25, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 525, NULL, NULL),

                -- Unrelated rows (using er_er_id and bu_bu_id, loc_loc_id is NULL)
                (26, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, NULL, 1001, NULL),
                (27, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, NULL, NULL, 2001),
                (28, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, 2002),
                (29, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, NULL, 1002, NULL),
                (30, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, NULL, NULL, 2003),
                (31, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, NULL, NULL, 2004),
                (32, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, NULL, 1003, NULL),
                (33, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, 2005),
                (34, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, NULL, NULL, 2006),
                (35, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, NULL, 1004, NULL),
                (36, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, NULL, NULL, 2007),
                (37, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, NULL, NULL, 2008),
                (38, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, NULL, 1005, NULL),
                (39, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, NULL, NULL, 2009),
                (40, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, NULL, NULL, 2010),
                (41, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, NULL, 1006, NULL),
                (42, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, NULL, NULL, 2011),
                (43, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, 2012),
                (44, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, NULL, 1007, NULL),
                (45, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, NULL, NULL, 2013),
                (46, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, NULL, NULL, 2014),
                (47, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, NULL, 1008, NULL),
                (48, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, NULL, NULL, 2015),
                (49, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, NULL, NULL, 2016),
                (50, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, NULL, 1009, NULL),

                -- More rows, mixing all types
                (51, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 526, NULL, NULL),
                (52, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 527, NULL, NULL),
                (53, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 528, NULL, NULL),
                (54, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 529, NULL, NULL),
                (55, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 530, NULL, NULL),
                (56, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 531, NULL, NULL),
                (57, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 532, NULL, NULL),
                (58, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 533, NULL, NULL),
                (59, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 534, NULL, NULL),
                (60, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 535, NULL, NULL),
                (61, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 536, NULL, NULL),
                (62, 'EM', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 537, NULL, NULL),
                (63, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 538, NULL, NULL),
                (64, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 539, NULL, NULL),
                (65, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 540, NULL, NULL),
                (66, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 541, NULL, NULL),
                (67, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 542, NULL, NULL),
                (68, 'FX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 543, NULL, NULL),
                (69, 'DI', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 544, NULL, NULL),
                (70, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 545, NULL, NULL),
                (71, 'MT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 4, 546, NULL, NULL),
                (72, 'DX', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 5, 547, NULL, NULL),
                (73, 'FT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 1, 548, NULL, NULL),
                (74, 'HT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 549, NULL, NULL),
                (75, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 3, 550, NULL, NULL),
                (76, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 553, NULL, NULL),
                (77, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 565, NULL, NULL),
                (78, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 585, NULL, NULL),
                (79, 'BT', CURRENT_DATE, NULL, 1, 1, CURRENT_TIMESTAMP, 2, 595, NULL, NULL);


-- ADDRESSSES
-- Populate addresses with some rows
-- Populate addresses based on link_addresses.adr_adr_id (1-150), with simple synthetic data
INSERT INTO addresses (
        adr_id, line1, line2, line3, line4, line5, postcode,
        start_date, end_date, version_number, changed_by, changed_date, mcc_mcc_id
) VALUES
        (1, 'Line1 1', 'Line2 1', 'Line3 1', 'Line4 1', 'Line5 1', 'AA1 1AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (2, 'Line1 2', 'Line2 2', 'Line3 2', 'Line4 2', 'Line5 2', 'AA2 2AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (3, 'Line1 3', 'Line2 3', 'Line3 3', 'Line4 3', 'Line5 3', 'AA3 3AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (4, 'Line1 4', 'Line2 4', 'Line3 4', 'Line4 4', 'Line5 4', 'AA4 4AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (5, 'Line1 5', 'Line2 5', 'Line3 5', 'Line4 5', 'Line5 5', 'AA5 5AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (6, 'Line1 6', 'Line2 6', 'Line3 6', 'Line4 6', 'Line5 6', 'AA6 6AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (7, 'Line1 7', 'Line2 7', 'Line3 7', 'Line4 7', 'Line5 7', 'AA7 7AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (8, 'Line1 8', 'Line2 8', 'Line3 8', 'Line4 8', 'Line5 8', 'AA8 8AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (9, 'Line1 9', 'Line2 9', 'Line3 9', 'Line4 9', 'Line5 9', 'AA9 9AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (10, 'Line1 10', 'Line2 10', 'Line3 10', 'Line4 10', 'Line5 10', 'AA10 0AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (11, 'Line1 11', 'Line2 11', 'Line3 11', 'Line4 11', 'Line5 11', 'AA11 1AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (12, 'Line1 12', 'Line2 12', 'Line3 12', 'Line4 12', 'Line5 12', 'AA12 2AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (13, 'Line1 13', 'Line2 13', 'Line3 13', 'Line4 13', 'Line5 13', 'AA13 3AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (14, 'Line1 14', 'Line2 14', 'Line3 14', 'Line4 14', 'Line5 14', 'AA14 4AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (15, 'Line1 15', 'Line2 15', 'Line3 15', 'Line4 15', 'Line5 15', 'AA15 5AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (16, 'Line1 16', 'Line2 16', 'Line3 16', 'Line4 16', 'Line5 16', 'AA16 6AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (17, 'Line1 17', 'Line2 17', 'Line3 17', 'Line4 17', 'Line5 17', 'AA17 7AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (18, 'Line1 18', 'Line2 18', 'Line3 18', 'Line4 18', 'Line5 18', 'AA18 8AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (19, 'Line1 19', 'Line2 19', 'Line3 19', 'Line4 19', 'Line5 19', 'AA19 9AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (20, 'Line1 20', 'Line2 20', 'Line3 20', 'Line4 20', 'Line5 20', 'AA20 0AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (21, 'Line1 21', 'Line2 21', 'Line3 21', 'Line4 21', 'Line5 21', 'AA21 1AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (22, 'Line1 22', 'Line2 22', 'Line3 22', 'Line4 22', 'Line5 22', 'AA22 2AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (23, 'Line1 23', 'Line2 23', 'Line3 23', 'Line4 23', 'Line5 23', 'AA23 3AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (24, 'Line1 24', 'Line2 24', 'Line3 24', 'Line4 24', 'Line5 24', 'AA24 4AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (25, 'Line1 25', 'Line2 25', 'Line3 25', 'Line4 25', 'Line5 25', 'AA25 5AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (26, 'Line1 26', 'Line2 26', 'Line3 26', 'Line4 26', 'Line5 26', 'AA26 6AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (27, 'Line1 27', 'Line2 27', 'Line3 27', 'Line4 27', 'Line5 27', 'AA27 7AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (28, 'Line1 28', 'Line2 28', 'Line3 28', 'Line4 28', 'Line5 28', 'AA28 8AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (29, 'Line1 29', 'Line2 29', 'Line3 29', 'Line4 29', 'Line5 29', 'AA29 9AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (30, 'Line1 30', 'Line2 30', 'Line3 30', 'Line4 30', 'Line5 30', 'AA30 0AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (31, 'Line1 31', 'Line2 31', 'Line3 31', 'Line4 31', 'Line5 31', 'AA31 1AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (32, 'Line1 32', 'Line2 32', 'Line3 32', 'Line4 32', 'Line5 32', 'AA32 2AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (33, 'Line1 33', 'Line2 33', 'Line3 33', 'Line4 33', 'Line5 33', 'AA33 3AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (34, 'Line1 34', 'Line2 34', 'Line3 34', 'Line4 34', 'Line5 34', 'AA34 4AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (35, 'Line1 35', 'Line2 35', 'Line3 35', 'Line4 35', 'Line5 35', 'AA35 5AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (36, 'Line1 36', 'Line2 36', 'Line3 36', 'Line4 36', 'Line5 36', 'AA36 6AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (37, 'Line1 37', 'Line2 37', 'Line3 37', 'Line4 37', 'Line5 37', 'AA37 7AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (38, 'Line1 38', 'Line2 38', 'Line3 38', 'Line4 38', 'Line5 38', 'AA38 8AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (39, 'Line1 39', 'Line2 39', 'Line3 39', 'Line4 39', 'Line5 39', 'AA39 9AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (40, 'Line1 40', 'Line2 40', 'Line3 40', 'Line4 40', 'Line5 40', 'AA40 0AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (41, 'Line1 41', 'Line2 41', 'Line3 41', 'Line4 41', 'Line5 41', 'AA41 1AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (42, 'Line1 42', 'Line2 42', 'Line3 42', 'Line4 42', 'Line5 42', 'AA42 2AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (43, 'Line1 43', 'Line2 43', 'Line3 43', 'Line4 43', 'Line5 43', 'AA43 3AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (44, 'Line1 44', 'Line2 44', 'Line3 44', 'Line4 44', 'Line5 44', 'AA44 4AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (45, 'Line1 45', 'Line2 45', 'Line3 45', 'Line4 45', 'Line5 45', 'AA45 5AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (46, 'Line1 46', 'Line2 46', 'Line3 46', 'Line4 46', 'Line5 46', 'AA46 6AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (47, 'Line1 47', 'Line2 47', 'Line3 47', 'Line4 47', 'Line5 47', 'AA47 7AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (48, 'Line1 48', 'Line2 48', 'Line3 48', 'Line4 48', 'Line5 48', 'AA48 8AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (49, 'Line1 49', 'Line2 49', 'Line3 49', 'Line4 49', 'Line5 49', 'AA49 9AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (50, 'Line1 50', 'Line2 50', 'Line3 50', 'Line4 50', 'Line5 50', 'AA50 0AA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        -- 51-100: CY_CA type, postcode pattern BB
        (51, 'Line1 51', 'Line2 51', 'Line3 51', 'Line4 51', 'Line5 51', 'BB51 1BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (52, 'Line1 52', 'Line2 52', 'Line3 52', 'Line4 52', 'Line5 52', 'BB52 2BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (53, 'Line1 53', 'Line2 53', 'Line3 53', 'Line4 53', 'Line5 53', 'BB53 3BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (54, 'Line1 54', 'Line2 54', 'Line3 54', 'Line4 54', 'Line5 54', 'BB54 4BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (55, 'Line1 55', 'Line2 55', 'Line3 55', 'Line4 55', 'Line5 55', 'BB55 5BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (56, 'Line1 56', 'Line2 56', 'Line3 56', 'Line4 56', 'Line5 56', 'BB56 6BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (57, 'Line1 57', 'Line2 57', 'Line3 57', 'Line4 57', 'Line5 57', 'BB57 7BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (58, 'Line1 58', 'Line2 58', 'Line3 58', 'Line4 58', 'Line5 58', 'BB58 8BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (59, 'Line1 59', 'Line2 59', 'Line3 59', 'Line4 59', 'Line5 59', 'BB59 9BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (60, 'Line1 60', 'Line2 60', 'Line3 60', 'Line4 60', 'Line5 60', 'BB60 0BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (61, 'Line1 61', 'Line2 61', 'Line3 61', 'Line4 61', 'Line5 61', 'BB61 1BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (62, 'Line1 62', 'Line2 62', 'Line3 62', 'Line4 62', 'Line5 62', 'BB62 2BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (63, 'Line1 63', 'Line2 63', 'Line3 63', 'Line4 63', 'Line5 63', 'BB63 3BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (64, 'Line1 64', 'Line2 64', 'Line3 64', 'Line4 64', 'Line5 64', 'BB64 4BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (65, 'Line1 65', 'Line2 65', 'Line3 65', 'Line4 65', 'Line5 65', 'BB65 5BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (66, 'Line1 66', 'Line2 66', 'Line3 66', 'Line4 66', 'Line5 66', 'BB66 6BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (67, 'Line1 67', 'Line2 67', 'Line3 67', 'Line4 67', 'Line5 67', 'BB67 7BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (68, 'Line1 68', 'Line2 68', 'Line3 68', 'Line4 68', 'Line5 68', 'BB68 8BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (69, 'Line1 69', 'Line2 69', 'Line3 69', 'Line4 69', 'Line5 69', 'BB69 9BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (70, 'Line1 70', 'Line2 70', 'Line3 70', 'Line4 70', 'Line5 70', 'BB70 0BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (71, 'Line1 71', 'Line2 71', 'Line3 71', 'Line4 71', 'Line5 71', 'BB71 1BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (72, 'Line1 72', 'Line2 72', 'Line3 72', 'Line4 72', 'Line5 72', 'BB72 2BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (73, 'Line1 73', 'Line2 73', 'Line3 73', 'Line4 73', 'Line5 73', 'BB73 3BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (74, 'Line1 74', 'Line2 74', 'Line3 74', 'Line4 74', 'Line5 74', 'BB74 4BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (75, 'Line1 75', 'Line2 75', 'Line3 75', 'Line4 75', 'Line5 75', 'BB75 5BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (76, 'Line1 76', 'Line2 76', 'Line3 76', 'Line4 76', 'Line5 76', 'BB76 6BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (77, 'Line1 77', 'Line2 77', 'Line3 77', 'Line4 77', 'Line5 77', 'BB77 7BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (78, 'Line1 78', 'Line2 78', 'Line3 78', 'Line4 78', 'Line5 78', 'BB78 8BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (79, 'Line1 79', 'Line2 79', 'Line3 79', 'Line4 79', 'Line5 79', 'BB79 9BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (80, 'Line1 80', 'Line2 80', 'Line3 80', 'Line4 80', 'Line5 80', 'BB80 0BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (81, 'Line1 81', 'Line2 81', 'Line3 81', 'Line4 81', 'Line5 81', 'BB81 1BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (82, 'Line1 82', 'Line2 82', 'Line3 82', 'Line4 82', 'Line5 82', 'BB82 2BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (83, 'Line1 83', 'Line2 83', 'Line3 83', 'Line4 83', 'Line5 83', 'BB83 3BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (84, 'Line1 84', 'Line2 84', 'Line3 84', 'Line4 84', 'Line5 84', 'BB84 4BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (85, 'Line1 85', 'Line2 85', 'Line3 85', 'Line4 85', 'Line5 85', 'BB85 5BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (86, 'Line1 86', 'Line2 86', 'Line3 86', 'Line4 86', 'Line5 86', 'BB86 6BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (87, 'Line1 87', 'Line2 87', 'Line3 87', 'Line4 87', 'Line5 87', 'BB87 7BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (88, 'Line1 88', 'Line2 88', 'Line3 88', 'Line4 88', 'Line5 88', 'BB88 8BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (89, 'Line1 89', 'Line2 89', 'Line3 89', 'Line4 89', 'Line5 89', 'BB89 9BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (90, 'Line1 90', 'Line2 90', 'Line3 90', 'Line4 90', 'Line5 90', 'BB90 0BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (91, 'Line1 91', 'Line2 91', 'Line3 91', 'Line4 91', 'Line5 91', 'BB91 1BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (92, 'Line1 92', 'Line2 92', 'Line3 92', 'Line4 92', 'Line5 92', 'BB92 2BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (93, 'Line1 93', 'Line2 93', 'Line3 93', 'Line4 93', 'Line5 93', 'BB93 3BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (94, 'Line1 94', 'Line2 94', 'Line3 94', 'Line4 94', 'Line5 94', 'BB94 4BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (95, 'Line1 95', 'Line2 95', 'Line3 95', 'Line4 95', 'Line5 95', 'BB95 5BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (96, 'Line1 96', 'Line2 96', 'Line3 96', 'Line4 96', 'Line5 96', 'BB96 6BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (97, 'Line1 97', 'Line2 97', 'Line3 97', 'Line4 97', 'Line5 97', 'BB97 7BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (98, 'Line1 98', 'Line2 98', 'Line3 98', 'Line4 98', 'Line5 98', 'BB98 8BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (99, 'Line1 99', 'Line2 99', 'Line3 99', 'Line4 99', 'Line5 99', 'BB99 9BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (100, 'Line1 100', 'Line2 100', 'Line3 100', 'Line4 100', 'Line5 100', 'BB00 0BB', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        -- 101-150: CFU type, postcode pattern CC/DD
        (101, 'Line1 101', 'Line2 101', 'Line3 101', 'Line4 101', 'Line5 101', 'CC01 1CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (102, 'Line1 102', 'Line2 102', 'Line3 102', 'Line4 102', 'Line5 102', 'CC02 2CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (103, 'Line1 103', 'Line2 103', 'Line3 103', 'Line4 103', 'Line5 103', 'CC03 3CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (104, 'Line1 104', 'Line2 104', 'Line3 104', 'Line4 104', 'Line5 104', 'CC04 4CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (105, 'Line1 105', 'Line2 105', 'Line3 105', 'Line4 105', 'Line5 105', 'CC05 5CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (106, 'Line1 106', 'Line2 106', 'Line3 106', 'Line4 106', 'Line5 106', 'CC06 6CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (107, 'Line1 107', 'Line2 107', 'Line3 107', 'Line4 107', 'Line5 107', 'CC07 7CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (108, 'Line1 108', 'Line2 108', 'Line3 108', 'Line4 108', 'Line5 108', 'CC08 8CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (109, 'Line1 109', 'Line2 109', 'Line3 109', 'Line4 109', 'Line5 109', 'CC09 9CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (110, 'Line1 110', 'Line2 110', 'Line3 110', 'Line4 110', 'Line5 110', 'CC10 0CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (111, 'Line1 111', 'Line2 111', 'Line3 111', 'Line4 111', 'Line5 111', 'CC11 1CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (112, 'Line1 112', 'Line2 112', 'Line3 112', 'Line4 112', 'Line5 112', 'CC12 2CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (113, 'Line1 113', 'Line2 113', 'Line3 113', 'Line4 113', 'Line5 113', 'CC13 3CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (114, 'Line1 114', 'Line2 114', 'Line3 114', 'Line4 114', 'Line5 114', 'CC14 4CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (115, 'Line1 115', 'Line2 115', 'Line3 115', 'Line4 115', 'Line5 115', 'CC15 5CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (116, 'Line1 116', 'Line2 116', 'Line3 116', 'Line4 116', 'Line5 116', 'CC16 6CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (117, 'Line1 117', 'Line2 117', 'Line3 117', 'Line4 117', 'Line5 117', 'CC17 7CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (118, 'Line1 118', 'Line2 118', 'Line3 118', 'Line4 118', 'Line5 118', 'CC18 8CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (119, 'Line1 119', 'Line2 119', 'Line3 119', 'Line4 119', 'Line5 119', 'CC19 9CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (120, 'Line1 120', 'Line2 120', 'Line3 120', 'Line4 120', 'Line5 120', 'CC20 0CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (121, 'Line1 121', 'Line2 121', 'Line3 121', 'Line4 121', 'Line5 121', 'CC21 1CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (122, 'Line1 122', 'Line2 122', 'Line3 122', 'Line4 122', 'Line5 122', 'CC22 2CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (123, 'Line1 123', 'Line2 123', 'Line3 123', 'Line4 123', 'Line5 123', 'CC23 3CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (124, 'Line1 124', 'Line2 124', 'Line3 124', 'Line4 124', 'Line5 124', 'CC24 4CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (125, 'Line1 125', 'Line2 125', 'Line3 125', 'Line4 125', 'Line5 125', 'CC25 5CC', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (126, 'Line1 126', 'Line2 126', 'Line3 126', 'Line4 126', 'Line5 126', 'DD01 1DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (127, 'Line1 127', 'Line2 127', 'Line3 127', 'Line4 127', 'Line5 127', 'DD02 2DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (128, 'Line1 128', 'Line2 128', 'Line3 128', 'Line4 128', 'Line5 128', 'DD03 3DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (129, 'Line1 129', 'Line2 129', 'Line3 129', 'Line4 129', 'Line5 129', 'DD04 4DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (130, 'Line1 130', 'Line2 130', 'Line3 130', 'Line4 130', 'Line5 130', 'DD05 5DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (131, 'Line1 131', 'Line2 131', 'Line3 131', 'Line4 131', 'Line5 131', 'DD06 6DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (132, 'Line1 132', 'Line2 132', 'Line3 132', 'Line4 132', 'Line5 132', 'DD07 7DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (133, 'Line1 133', 'Line2 133', 'Line3 133', 'Line4 133', 'Line5 133', 'DD08 8DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (134, 'Line1 134', 'Line2 134', 'Line3 134', 'Line4 134', 'Line5 134', 'DD09 9DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (135, 'Line1 135', 'Line2 135', 'Line3 135', 'Line4 135', 'Line5 135', 'DD10 0DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (136, 'Line1 136', 'Line2 136', 'Line3 136', 'Line4 136', 'Line5 136', 'DD11 1DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (137, 'Line1 137', 'Line2 137', 'Line3 137', 'Line4 137', 'Line5 137', 'DD12 2DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (138, 'Line1 138', 'Line2 138', 'Line3 138', 'Line4 138', 'Line5 138', 'DD13 3DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (139, 'Line1 139', 'Line2 139', 'Line3 139', 'Line4 139', 'Line5 139', 'DD14 4DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (140, 'Line1 140', 'Line2 140', 'Line3 140', 'Line4 140', 'Line5 140', 'DD15 5DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (141, 'Line1 141', 'Line2 141', 'Line3 141', 'Line4 141', 'Line5 141', 'DD16 6DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (142, 'Line1 142', 'Line2 142', 'Line3 142', 'Line4 142', 'Line5 142', 'DD17 7DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (143, 'Line1 143', 'Line2 143', 'Line3 143', 'Line4 143', 'Line5 143', 'DD18 8DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (144, 'Line1 144', 'Line2 144', 'Line3 144', 'Line4 144', 'Line5 144', 'DD19 9DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (145, 'Line1 145', 'Line2 145', 'Line3 145', 'Line4 145', 'Line5 145', 'DD20 0DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (146, 'Line1 146', 'Line2 146', 'Line3 146', 'Line4 146', 'Line5 146', 'DD21 1DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (147, 'Line1 147', 'Line2 147', 'Line3 147', 'Line4 147', 'Line5 147', 'DD22 2DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (148, 'Line1 148', 'Line2 148', 'Line3 148', 'Line4 148', 'Line5 148', 'DD23 3DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (149, 'Line1 149', 'Line2 149', 'Line3 149', 'Line4 149', 'Line5 149', 'DD24 4DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL),
        (150, 'Line1 150', 'Line2 150', 'Line3 150', 'Line4 150', 'Line5 150', 'DD25 5DD', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, NULL);

-- LINK_ADDRESSES
-- Populate link_addresses with some rows referencing national_court_houses.loc_loc_id with different la_type values
-- Populate link_addresses with approx 150 rows, referencing national_court_houses.loc_loc_id (501-600) and some with bu_bu_id or er_er_id
INSERT INTO link_addresses (
        la_id, no_fixed_abode, la_type, start_date, end_date, version_number, changed_by, changed_date,
        adr_adr_id, bu_bu_id, er_er_id, loc_loc_id, head_office_indicator
) VALUES
        -- 50 rows referencing national_court_houses.loc_loc_id (501-550), la_type 'CA'
        (1, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 1, NULL, NULL, 501, 'Y'),
        (2, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 2, NULL, NULL, 502, NULL),
        (3, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 3, NULL, NULL, 503, NULL),
        (4, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 4, NULL, NULL, 504, NULL),
        (5, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 5, NULL, NULL, 505, NULL),
        (6, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 6, NULL, NULL, 506, NULL),
        (7, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 7, NULL, NULL, 507, NULL),
        (8, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 8, NULL, NULL, 508, NULL),
        (9, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 9, NULL, NULL, 509, NULL),
        (10, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 10, NULL, NULL, 510, NULL),
        (11, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 11, NULL, NULL, 511, NULL),
        (12, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 12, NULL, NULL, 512, NULL),
        (13, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 13, NULL, NULL, 513, NULL),
        (14, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 14, NULL, NULL, 514, NULL),
        (15, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 15, NULL, NULL, 515, NULL),
        (16, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 16, NULL, NULL, 516, NULL),
        (17, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 17, NULL, NULL, 517, NULL),
        (18, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 18, NULL, NULL, 518, NULL),
        (19, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 19, NULL, NULL, 519, NULL),
        (20, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 20, NULL, NULL, 520, NULL),
        (21, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 21, NULL, NULL, 521, NULL),
        (22, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 22, NULL, NULL, 522, NULL),
        (23, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 23, NULL, NULL, 523, NULL),
        (24, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 24, NULL, NULL, 524, NULL),
        (25, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 25, NULL, NULL, 525, NULL),
        (26, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 26, NULL, NULL, 526, NULL),
        (27, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 27, NULL, NULL, 527, NULL),
        (28, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 28, NULL, NULL, 528, NULL),
        (29, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 29, NULL, NULL, 529, NULL),
        (30, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 30, NULL, NULL, 530, NULL),
        (31, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 31, NULL, NULL, 531, NULL),
        (32, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 32, NULL, NULL, 532, NULL),
        (33, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 33, NULL, NULL, 533, NULL),
        (34, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 34, NULL, NULL, 534, NULL),
        (35, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 35, NULL, NULL, 535, NULL),
        (36, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 36, NULL, NULL, 536, NULL),
        (37, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 37, NULL, NULL, 537, NULL),
        (38, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 38, NULL, NULL, 538, NULL),
        (39, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 39, NULL, NULL, 539, NULL),
        (40, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 40, NULL, NULL, 540, NULL),
        (41, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 41, NULL, NULL, 541, NULL),
        (42, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 42, NULL, NULL, 542, NULL),
        (43, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 43, NULL, NULL, 543, NULL),
        (44, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 44, NULL, NULL, 544, NULL),
        (45, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 45, NULL, NULL, 545, NULL),
        (46, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 46, NULL, NULL, 546, NULL),
        (47, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 47, NULL, NULL, 547, NULL),
        (48, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 48, NULL, NULL, 548, NULL),
        (49, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 49, NULL, NULL, 549, NULL),
        (50, 'N', 'CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 50, NULL, NULL, 550, NULL),

        -- 50 rows referencing national_court_houses.loc_loc_id (551-600), la_type 'CY_CA'
        (51, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 51, NULL, NULL, 551, NULL),
        (52, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 52, NULL, NULL, 552, NULL),
        (53, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 53, NULL, NULL, 553, NULL),
        (54, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 54, NULL, NULL, 554, NULL),
        (55, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 55, NULL, NULL, 555, NULL),
        (56, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 56, NULL, NULL, 556, NULL),
        (57, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 57, NULL, NULL, 557, NULL),
        (58, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 58, NULL, NULL, 558, NULL),
        (59, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 59, NULL, NULL, 559, NULL),
        (60, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 60, NULL, NULL, 560, NULL),
        (61, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 61, NULL, NULL, 561, NULL),
        (62, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 62, NULL, NULL, 562, NULL),
        (63, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 63, NULL, NULL, 563, NULL),
        (64, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 64, NULL, NULL, 564, NULL),
        (65, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 65, NULL, NULL, 565, NULL),
        (66, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 66, NULL, NULL, 566, NULL),
        (67, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 67, NULL, NULL, 567, NULL),
        (68, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 68, NULL, NULL, 568, NULL),
        (69, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 69, NULL, NULL, 569, NULL),
        (70, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 70, NULL, NULL, 570, NULL),
        (71, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 71, NULL, NULL, 571, NULL),
        (72, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 72, NULL, NULL, 572, NULL),
        (73, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 73, NULL, NULL, 573, NULL),
        (74, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 74, NULL, NULL, 574, NULL),
        (75, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 75, NULL, NULL, 575, NULL),
        (76, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 76, NULL, NULL, 576, NULL),
        (77, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 77, NULL, NULL, 577, NULL),
        (78, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 78, NULL, NULL, 578, NULL),
        (79, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 79, NULL, NULL, 579, NULL),
        (80, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 80, NULL, NULL, 580, NULL),
        (81, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 81, NULL, NULL, 581, NULL),
        (82, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 82, NULL, NULL, 582, NULL),
        (83, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 83, NULL, NULL, 583, NULL),
        (84, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 84, NULL, NULL, 584, NULL),
        (85, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 85, NULL, NULL, 585, NULL),
        (86, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 86, NULL, NULL, 586, NULL),
        (87, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 87, NULL, NULL, 587, NULL),
        (88, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 88, NULL, NULL, 588, NULL),
        (89, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 89, NULL, NULL, 589, NULL),
        (90, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 90, NULL, NULL, 590, NULL),
        (91, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 91, NULL, NULL, 591, NULL),
        (92, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 92, NULL, NULL, 592, NULL),
        (93, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 93, NULL, NULL, 593, NULL),
        (94, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 94, NULL, NULL, 594, NULL),
        (95, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 95, NULL, NULL, 595, NULL),
        (96, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 96, NULL, NULL, 596, NULL),
        (97, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 97, NULL, NULL, 597, NULL),
        (98, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 98, NULL, NULL, 598, NULL),
        (99, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 99, NULL, NULL, 599, NULL),
        (100, 'N', 'CY_CA', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 100, NULL, NULL, 600, NULL),

        -- 25 rows with bu_bu_id, la_type 'CFU'
        (101, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 101, 2001, NULL, NULL, NULL),
        (102, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 102, 2002, NULL, NULL, NULL),
        (103, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 103, 2003, NULL, NULL, NULL),
        (104, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 104, 2004, NULL, NULL, NULL),
        (105, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 105, 2005, NULL, NULL, NULL),
        (106, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 106, 2006, NULL, NULL, NULL),
        (107, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 107, 2007, NULL, NULL, NULL),
        (108, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 108, 2008, NULL, NULL, NULL),
        (109, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 109, 2009, NULL, NULL, NULL),
        (110, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 110, 2010, NULL, NULL, NULL),
        (111, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 111, 2011, NULL, NULL, NULL),
        (112, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 112, 2012, NULL, NULL, NULL),
        (113, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 113, 2013, NULL, NULL, NULL),
        (114, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 114, 2014, NULL, NULL, NULL),
        (115, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 115, 2015, NULL, NULL, NULL),
        (116, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 116, 2016, NULL, NULL, NULL),
        (117, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 117, 2017, NULL, NULL, NULL),
        (118, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 118, 2018, NULL, NULL, NULL),
        (119, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 119, 2019, NULL, NULL, NULL),
        (120, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 120, 2020, NULL, NULL, NULL),
        (121, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 121, 2021, NULL, NULL, NULL),
        (122, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 122, 2022, NULL, NULL, NULL),
        (123, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 123, 2023, NULL, NULL, NULL),
        (124, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 124, 2024, NULL, NULL, NULL),
        (125, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 125, 2025, NULL, NULL, NULL),

        -- 25 rows with er_er_id, la_type 'CFU'
        (126, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 126, NULL, 3001, NULL, NULL),
        (127, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 127, NULL, 3002, NULL, NULL),
        (128, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 128, NULL, 3003, NULL, NULL),
        (129, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 129, NULL, 3004, NULL, NULL),
        (130, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 130, NULL, 3005, NULL, NULL),
        (131, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 131, NULL, 3006, NULL, NULL),
        (132, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 132, NULL, 3007, NULL, NULL),
        (133, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 133, NULL, 3008, NULL, NULL),
        (134, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 134, NULL, 3009, NULL, NULL),
        (135, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 135, NULL, 3010, NULL, NULL),
        (136, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 136, NULL, 3011, NULL, NULL),
        (137, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 137, NULL, 3012, NULL, NULL),
        (138, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 138, NULL, 3013, NULL, NULL),
        (139, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 139, NULL, 3014, NULL, NULL),
        (140, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 140, NULL, 3015, NULL, NULL),
        (141, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 141, NULL, 3016, NULL, NULL),
        (142, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 142, NULL, 3017, NULL, NULL),
        (143, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 143, NULL, 3018, NULL, NULL),
        (144, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 144, NULL, 3019, NULL, NULL),
        (145, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 145, NULL, 3020, NULL, NULL),
        (146, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 146, NULL, 3021, NULL, NULL),
        (147, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 147, NULL, 3022, NULL, NULL),
        (148, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 148, NULL, 3023, NULL, NULL),
        (149, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 149, NULL, 3024, NULL, NULL),
        (150, 'N', 'CFU', CURRENT_DATE, NULL, 1, 1, CURRENT_DATE, 150, NULL, 3025, NULL, NULL);

-- APP_LIST_ENTRY_OFFICIAL
-- Populate app_list_entry_official with sample data for the first 20 application_list_entries
INSERT INTO app_list_entry_official (
        aleo_id, ale_ale_id, title, forename, surname, official_type, changed_by, changed_date, user_name
) VALUES
        (1, 1, 'Mr', 'John', 'Smith', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (2, 2, 'Ms', 'Jane', 'Doe', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (3, 3, 'Dr', 'Alex', 'Dunn', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (4, 4, 'Mrs', 'Sarah', 'Johnson', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (5, 5, 'Mr', 'Jack', 'Turner', 'M', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (6, 6, 'Judge', 'Alice', 'Brown', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (7, 7, 'Judge', 'Bob', 'Green', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (8, 8, 'Judge', 'Carol', 'White', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (9, 9, 'Judge', 'David', 'Black', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (10, 10, 'Judge', 'Emma', 'Grey', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (11, 11, 'Judge', 'Frank', 'Blue', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (12, 12, 'Judge', 'Grace', 'Red', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (13, 13, 'Judge', 'Henry', 'Violet', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (14, 14, 'Judge', 'Ivy', 'Orange', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (15, 15, 'Judge', 'Jack', 'Indigo', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (16, 16, 'Judge', 'Kelly', 'Pink', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (17, 17, 'Judge', 'Liam', 'Teal', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (18, 18, 'Judge', 'Mona', 'Gold', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (19, 19, 'Judge', 'Nina', 'Silver', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20, 20, 'Judge', 'Oscar', 'Bronze', 'C', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload');

-- APPLICATION_REGISTER
-- Populate application_register with 20 sample rows referencing application_lists
INSERT INTO application_register (
        ar_id, al_al_id, text, changed_by, changed_date, user_name
) VALUES
        (1, 1, 'Application registered for Royal Courts of Justice Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (2, 2, 'Application registered for Manchester Civil Justice Centre Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (3, 3, 'Application registered for Cardiff Crown Court Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (4, 4, 'Application registered for Birmingham Civil Justice Centre Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (5, 5, 'Application registered for Leeds Combined Court Centre Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (6, 6, 'Application registered for Bristol Crown Court Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (7, 7, 'Application registered for Liverpool Crown Court Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (8, 8, 'Application registered for Nottingham Justice Centre Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (9, 9, 'Application registered for Sheffield Combined Court Centre Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (10, 10, 'Application registered for Newcastle Crown Court Set 1', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (11, 11, 'Application registered for Royal Courts of Justice Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (12, 12, 'Application registered for Manchester Civil Justice Centre Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (13, 13, 'Application registered for Cardiff Crown Court Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (14, 14, 'Application registered for Birmingham Civil Justice Centre Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (15, 15, 'Application registered for Leeds Combined Court Centre Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (16, 16, 'Application registered for Bristol Crown Court Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (17, 17, 'Application registered for Liverpool Crown Court Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (18, 18, 'Application registered for Nottingham Justice Centre Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (19, 19, 'Application registered for Sheffield Combined Court Centre Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (20, 20, 'Application registered for Newcastle Crown Court Set 2', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (21, 21, 'Application registered for Royal Courts of Justice Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (22, 22, 'Application registered for Manchester Civil Justice Centre Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (23, 23, 'Application registered for Cardiff Crown Court Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (24, 24, 'Application registered for Birmingham Civil Justice Centre Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (25, 25, 'Application registered for Leeds Combined Court Centre Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (26, 26, 'Application registered for Bristol Crown Court Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (27, 27, 'Application registered for Liverpool Crown Court Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (28, 28, 'Application registered for Nottingham Justice Centre Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (29, 29, 'Application registered for Sheffield Combined Court Centre Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (30, 30, 'Application registered for Newcastle Crown Court Set 3', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (31, 31, 'Application registered for Royal Courts of Justice Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (32, 32, 'Application registered for Manchester Civil Justice Centre Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (33, 33, 'Application registered for Cardiff Crown Court Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (34, 34, 'Application registered for Birmingham Civil Justice Centre Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (35, 35, 'Application registered for Leeds Combined Court Centre Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (36, 36, 'Application registered for Bristol Crown Court Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (37, 37, 'Application registered for Liverpool Crown Court Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (38, 38, 'Application registered for Nottingham Justice Centre Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (39, 39, 'Application registered for Sheffield Combined Court Centre Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (40, 40, 'Application registered for Newcastle Crown Court Set 4', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (41, 41, 'Application registered for Royal Courts of Justice Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (42, 42, 'Application registered for Manchester Civil Justice Centre Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (43, 43, 'Application registered for Cardiff Crown Court Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (44, 44, 'Application registered for Birmingham Civil Justice Centre Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (45, 45, 'Application registered for Leeds Combined Court Centre Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (46, 46, 'Application registered for Bristol Crown Court Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (47, 47, 'Application registered for Liverpool Crown Court Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (48, 48, 'Application registered for Nottingham Justice Centre Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (49, 49, 'Application registered for Sheffield Combined Court Centre Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (50, 50, 'Application registered for Newcastle Crown Court Set 5', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (51, 51, 'Application registered for Royal Courts of Justice Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (52, 52, 'Application registered for Manchester Civil Justice Centre Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (53, 53, 'Application registered for Cardiff Crown Court Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (54, 54, 'Application registered for Birmingham Civil Justice Centre Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (55, 55, 'Application registered for Leeds Combined Court Centre Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (56, 56, 'Application registered for Bristol Crown Court Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (57, 57, 'Application registered for Liverpool Crown Court Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (58, 58, 'Application registered for Nottingham Justice Centre Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (59, 59, 'Application registered for Sheffield Combined Court Centre Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (60, 60, 'Application registered for Newcastle Crown Court Set 6', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (61, 61, 'Application registered for Royal Courts of Justice Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (62, 62, 'Application registered for Manchester Civil Justice Centre Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (63, 63, 'Application registered for Cardiff Crown Court Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (64, 64, 'Application registered for Birmingham Civil Justice Centre Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (65, 65, 'Application registered for Leeds Combined Court Centre Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (66, 66, 'Application registered for Bristol Crown Court Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (67, 67, 'Application registered for Liverpool Crown Court Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (68, 68, 'Application registered for Nottingham Justice Centre Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (69, 69, 'Application registered for Sheffield Combined Court Centre Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (70, 70, 'Application registered for Newcastle Crown Court Set 7', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (71, 71, 'Application registered for Royal Courts of Justice Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (72, 72, 'Application registered for Manchester Civil Justice Centre Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (73, 73, 'Application registered for Cardiff Crown Court Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (74, 74, 'Application registered for Birmingham Civil Justice Centre Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (75, 75, 'Application registered for Leeds Combined Court Centre Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (76, 76, 'Application registered for Bristol Crown Court Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (77, 77, 'Application registered for Liverpool Crown Court Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (78, 78, 'Application registered for Nottingham Justice Centre Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (79, 79, 'Application registered for Sheffield Combined Court Centre Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (80, 80, 'Application registered for Newcastle Crown Court Set 8', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (81, 81, 'Application registered for Royal Courts of Justice Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (82, 82, 'Application registered for Manchester Civil Justice Centre Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (83, 83, 'Application registered for Cardiff Crown Court Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (84, 84, 'Application registered for Birmingham Civil Justice Centre Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (85, 85, 'Application registered for Leeds Combined Court Centre Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (86, 86, 'Application registered for Bristol Crown Court Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (87, 87, 'Application registered for Liverpool Crown Court Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (88, 88, 'Application registered for Nottingham Justice Centre Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (89, 89, 'Application registered for Sheffield Combined Court Centre Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (90, 90, 'Application registered for Newcastle Crown Court Set 9', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (91, 91, 'Application registered for Royal Courts of Justice Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (92, 92, 'Application registered for Manchester Civil Justice Centre Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (93, 93, 'Application registered for Cardiff Crown Court Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (94, 94, 'Application registered for Birmingham Civil Justice Centre Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (95, 95, 'Application registered for Leeds Combined Court Centre Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (96, 96, 'Application registered for Bristol Crown Court Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (97, 97, 'Application registered for Liverpool Crown Court Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (98, 98, 'Application registered for Nottingham Justice Centre Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (99, 99, 'Application registered for Sheffield Combined Court Centre Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload'),
        (100, 100, 'Application registered for Newcastle Crown Court Set 10', gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'AR4.Initial.SQL.Upload');


-- GENERATE SPECIFIC DATA FOR THE TESTS THAT WE WILL RUN AS PART OF THE LOAD
        -- Insert a row into application_lists that matches the query criteria
        INSERT INTO application_lists (
                al_id, application_list_status, application_list_date, application_list_time, courthouse_code, other_courthouse,
                list_description, version, changed_by, changed_date, user_name, courthouse_name, duration_hour, duration_minute, cja_cja_id
        ) VALUES (
                101, -- next unused al_id
                'OPEN',
                CURRENT_DATE + INTERVAL '10 days', -- within 3-21 days in future
                CURRENT_TIME,
                'RCJ001',
                NULL,
                'ENFORCEMENT LIST - TEST',
                1,
                gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47',
                CURRENT_DATE,
                '12345678-aaaa-bbbb-cccc-00000000101',
                'Royal Courts of Justice Set 1',
                2,
                0,
                1
        );

        -- Ensure there are <= 20 entries for this list (so NOT EXISTS subquery is true)
        INSERT INTO application_list_entries (
                ale_id, al_al_id, sa_sa_id, ac_ac_id, a_na_id, r_na_id, number_of_bulk_respondents,
                application_list_entry_wording, case_reference, entry_rescheduled, notes, version,
                changed_by, changed_date, bulk_upload, sequence_number, lodgement_date
        )
        WITH numbered_entries AS (
            SELECT
                ROW_NUMBER() OVER () AS rn,
                sa.sa_id,
                ac.ac_id,
                ac.application_code_title
            FROM standard_applicants sa
            JOIN application_codes ac ON ac.ac_id <= 2 -- only 2 codes, so max 20 rows (10 applicants x 2 codes)
            WHERE sa.sa_id <= 10
            LIMIT 20
        ),
        max_ale AS (
            SELECT COALESCE(MAX(ale_id), 0) AS max_id FROM application_list_entries
        )
        SELECT
            max_ale.max_id + ne.rn AS ale_id,
            101 AS al_al_id,
            ne.sa_id,
            ne.ac_id,
            NULL, NULL, 0,
            ne.application_code_title,
            CONCAT('CASE101-', LPAD(ne.sa_id::text, 3, '0'), '-', LPAD(ne.ac_id::text, 3, '0')),
            'N', NULL, 1, gen_random_uuid() || ':72f988bf-86f1-41af-91ab-2d7cd011db47', CURRENT_TIMESTAMP, 'N', 1, CURRENT_TIMESTAMP
        FROM numbered_entries ne
        CROSS JOIN max_ale;



        -- Reset all sequences to max(id) + 1 or 1 if table is empty
        -- add_seq for addresses table
         DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(adr_id), 0) + 1 INTO max_id FROM addresses;
                PERFORM setval('adr_seq', max_id, false);
        END$$;

        -- No sequence for app_list_entry_fee_id
        
        -- alefs_seq for app_list_entry_fee_status
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(alefs_id), 0) + 1 INTO max_id FROM app_list_entry_fee_status;
                PERFORM setval('alefs_seq', max_id, false);
        END$$;

        -- aleo_seq for app_list_entry_official
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(aleo_id), 0) + 1 INTO max_id FROM app_list_entry_official;
                PERFORM setval('aleo_seq', max_id, false);
        END$$;

        -- aler_seq for app_list_entry_resolutions
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(aler_id), 0) + 1 INTO max_id FROM app_list_entry_resolutions;
                PERFORM setval('aler_seq', max_id, false);
        END$$;

        -- ac_seq for application_codes
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(ac_id), 0) + 1 INTO max_id FROM application_codes;
                PERFORM setval('ac_seq', max_id, false);
        END$$;

        -- ale_seq for application_list_entries
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(ale_id), 0) + 1 INTO max_id FROM application_list_entries;
                PERFORM setval('ale_seq', max_id, false);
        END$$;

        -- al_seq for application_lists
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(al_id), 0) + 1 INTO max_id FROM application_lists;
                PERFORM setval('al_seq', max_id, false);
        END$$;

        -- ar_seq for application_register
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(ar_id), 0) + 1 INTO max_id FROM application_register;
                PERFORM setval('ar_seq', max_id, false);
        END$$;

        -- comm_seq for communication_media 
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(comm_id), 0) + 1 INTO max_id FROM communication_media;
                PERFORM setval('comm_seq', max_id, false);
        END$$;

        -- cja_seq for criminal_justice_area
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(cja_id), 0) + 1 INTO max_id FROM criminal_justice_area;
                PERFORM setval('cja_seq', max_id, false);
        END$$;

        -- fee_seq for fee
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(fee_id), 0) + 1 INTO max_id FROM fee;
                PERFORM setval('fee_seq', max_id, false);
        END$$;

        -- la_seq for link_addresses
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(la_id), 0) + 1 INTO max_id FROM link_addresses;
                PERFORM setval('la_seq', max_id, false);
        END$$;

        -- lcm_seq for link_communication_media
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(lcm_id), 0) + 1 INTO max_id FROM link_communication_media;
                PERFORM setval('lcm_seq', max_id, false);
        END$$;

        -- na_seq for name_address
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(na_id), 0) + 1 INTO max_id FROM name_address;
                PERFORM setval('na_seq', max_id, false);
        END$$;

        -- nch_seq for national_court_houses
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(nch_id), 0) + 1 INTO max_id FROM national_court_houses;
                PERFORM setval('nch_seq', max_id, false);
        END$$;

        -- psa_seq for petty_sessional_areas
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(psa_id), 0) + 1 INTO max_id FROM petty_sessional_areas;
                PERFORM setval('psa_seq', max_id, false);
        END$$;

        -- rc_seq for resolution_codes
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(rc_id), 0) + 1 INTO max_id FROM resolution_codes;
                PERFORM setval('rc_seq', max_id, false);
        END$$;

        -- sa_seq for standard_applicants
        DO $$
        DECLARE
                max_id bigint;
        BEGIN
                SELECT COALESCE(MAX(sa_id), 0) + 1 INTO max_id FROM standard_applicants;
                PERFORM setval('sa_seq', max_id, false);
        END$$;

-- Insert our test data for V4
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('4', 'test_support', 'check_data_expected_v4_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_data_expected_v4_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
        -- Check for application_codes entry 'AD99001' active
	IF NOT EXISTS (select 1 FROM application_codes ac where ac.application_code = 'AD99001' and (ac.application_code_end_date is null or current_timestamp between ac.application_code_start_date and ac.application_code_end_date)) THEN
		RAISE EXCEPTION 'Application_Codes entry "AD99001" active check fails';
	END IF;
        -- Check for application_list_entries joined to app_list_entry_resolutions 
	IF NOT EXISTS (select 1 FROM application_list_entries as ale join app_list_entry_resolutions as aler on aler.ale_ale_id = ale.ale_id where ale.ale_id = 1 and ale.version = 1 limit 1) THEN
		RAISE EXCEPTION 'Application_list_entries joined to app_list_entry_resolutions check fails';
	END IF;
        -- Check for application_list_entries joined to application_codes and name_address
	IF NOT EXISTS (SELECT 1 FROM application_list_entries AS ale2 LEFT JOIN (SELECT ale1.ale_id, 'MO'::text AS tcep_status FROM application_list_entries AS ale1 JOIN application_codes AS ac ON ac.ac_id = ale1.ac_ac_id JOIN name_address AS na ON na.na_id = ale1.r_na_id WHERE ale1.ale_id = 1 AND ac.application_code LIKE 'EF%' AND ale1.user_name = 'TCEP_DMS' AND na.dms_id IS NOT NULL) AS tr ON ale2.ale_id = tr.ale_id WHERE ale2.ale_id = 1) THEN
		RAISE EXCEPTION 'Application_list_entries joined to application_codes and name_address check fails';
	END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
INSERT INTO standard_applicants (sa_id, standard_applicant_code, standard_applicant_start_date, standard_applicant_end_date,
                                 version,  changed_by, changed_date, user_name, name, title, forename_1, forename_2, forename_3, surname, address_l1,
                                 address_l2, address_l3, address_l4, address_l5, postcode, email_address, telephone_number, mobile_number) VALUES
                                                                                                                                               (5, 'APP005', CURRENT_TIMESTAMP - INTERVAL '1 day',
                                                                                                                                                NULL, 1, 0, CURRENT_TIMESTAMP, '12345678-aaaa-bbbb-cccc-000000000001',
                                                                                                                                                'Organisation 1', 'Mr', 'Owen', NULL, NULL, 'Jones','123 High Street',
                                                                                                                                                NULL, NULL, 'Townsville', NULL,'TS1 1AB', 'john.smith@example.com',
                                                                                                                                                '01234567890', '07123456789');

-- Alter tables after comparison with Oracle 

-- Version Control
-- V1.0  	Matthew Harman  29/08/2025	Initial Version
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

ALTER TABLE FEE ALTER COLUMN FEE_VALUE TYPE NUMERIC(9,2);

DROP SEQUENCE IF EXISTS ale_seq;
CREATE SEQUENCE ale_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2975601 CACHE 20;

DROP SEQUENCE IF EXISTS nch_seq;
CREATE SEQUENCE nch_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 9230 CACHE 20;

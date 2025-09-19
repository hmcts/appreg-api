-- Alter tables after comparison with Oracle 

-- Version Control
-- V1.0  	Matthew Harman  29/08/2025	Initial Version
-- V2.0  	Matthew Harman  16/09/2025	Added code for testing script applied
--
--

SET client_encoding TO 'UTF8';

SET check_function_bodies = false;

ALTER TABLE FEE ALTER COLUMN FEE_VALUE TYPE NUMERIC(9,2);

DROP SEQUENCE IF EXISTS ale_seq;
CREATE SEQUENCE ale_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 2975601 CACHE 20;

DROP SEQUENCE IF EXISTS nch_seq;
CREATE SEQUENCE nch_seq INCREMENT 1 MINVALUE 1 NO MAXVALUE START 9230 CACHE 20;

-- Insert our test data for V3
INSERT INTO test_support.test_registry (version, routine_schema, routine_name)
VALUES ('3', 'test_support', 'check_schema_objects_v3_present')
ON CONFLICT DO NOTHING;

-- Create the test as a function that RAISES EXCEPTION on failure
CREATE OR REPLACE FUNCTION test_support.check_schema_objects_v3_present()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
	-- Check for existence of fee.fee_value as a numeric field
	IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fee' and column_name = 'fee_value' and data_type = 'numeric') THEN
		RAISE EXCEPTION 'Table: fee  Column: fee_value is not a numeric field';
	END IF;

	-- If all checks pass, do nothing (test passes)
END $$;
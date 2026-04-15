-- migration_integrity.sql
--
-- The purpose of this script is for it to be run before a migration is
-- attempted.
-- It will flag all data in the Oracle database that does not conform to the
-- integrity and check rules that are on the postgres database.
-- The output should be reviewed pre-migration and any findings in the report
-- corrected.  A further run of the script should then be performed confirming
-- no issues are found.
--
-- Version History
-- Version	Date		Who		Purpose
-- 1.0		13/04/2026	Matthew Harman	Initial version 
--

-- First look for anything that will break the CHECK constraint:
-- alefs_fee_status_chk.   This makes sure alefs_fee_status contains one of the
--			   following values, D, P, R or U
--			   in the APP_LIST_ENTRY_FEE_STATUS
SET SERVEROUTPUT ON;
SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check app_list_entry_fee_status, specificially alefs_fee_status has a value not in D, P, R or U.');
END;
/

SET FEEDBACK ON;
SELECT ALEFS_ID, ALEFS_PAYMENT_REFERENCE, ALEFS_FEE_STATUS FROM appregister.app_list_entry_fee_status WHERE alefs_fee_status NOT IN ('D','P','R','U');

SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check app_list_entry_official, specificially official_type_chk has a value not in C or M.');
END;
/

SET FEEDBACK ON;
COL TITLE FORMAT a10
COL FORENAME FORMAT a20
COL SURNAME FORMAT a20
SELECT ALEO_ID, TITLE, FORENAME, SURNAME, OFFICIAL_TYPE FROM appregister.app_list_entry_official WHERE official_type NOT IN ('C','M');


SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check name_address, specificially check for organisation, i.e. NAME set, TITLE, FORENAME_1, FORENAME_2, FORENAME_3 and SURNAME not set.');
END;
/

SET FEEDBACK ON;
COL NAME FORMAT A10
COL TITLE FORMAT A10
COL FORENAME_1 FORMAT A20
COL FORENAME_2 FORMAT A20
COL FORENAME_3 FORMAT A20
COL SURNAME FORMAT A20
SELECT NA_ID, NAME, TITLE, FORENAME_1, FORENAME_2, FORENAME_3, SURNAME FROM appregister.name_address WHERE name IS NOT NULL AND (TITLE IS NOT NULL OR FORENAME_1 IS NOT NULL OR FORENAME_2 IS NOT NULL OR FORENAME_3 IS NOT NULL OR SURNAME IS NOT NULL);

SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check name_address, specificially check for person basic details set, i.e. NAME not set, FORENAME_1 and SURNAME are set.');
END;
/

SET FEEDBACK ON;
SELECT NA_ID, NAME, TITLE, FORENAME_1, FORENAME_2, FORENAME_3, SURNAME FROM appregister.name_address WHERE name IS NULL AND (FORENAME_1 IS NULL OR SURNAME IS NULL);

SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check name_address, specifically forename_2 is only set when forename_1 is set.');
END;
/

SET FEEDBACK ON;
SELECT NA_ID, NAME, TITLE, FORENAME_1, FORENAME_2, FORENAME_3, SURNAME FROM appregister.name_address WHERE forename_1 IS NULL AND FORENAME_2 IS NOT NULL;

SET FEEDBACK OFF;

BEGIN
	DBMS_OUTPUT.PUT_LINE('Check name_address, specifically forename_3 is only set when forename_1 is set.');
END;
/

SET FEEDBACK ON;
SELECT NA_ID, NAME, TITLE, FORENAME_1, FORENAME_2, FORENAME_3, SURNAME FROM appregister.name_address WHERE forename_1 IS NULL AND FORENAME_3 IS NOT NULL;


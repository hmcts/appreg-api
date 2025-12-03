#!/bin/bash

# Script:		extract_appreg_data.sh
#
# Purpose:		This script extracts all data from the Oracle database
#			pertaining to the App Reg product.  This extract can 
# 			then be used to populate the Postgres database as part 
# 			of App Reg modernisation
#
# Usage:		sh ./extract_appreg_data.sh
#
# Version History:
# Version	Date		Who		Purpose
# 1.0		14/08/2025	Matthew Harman	Initial Version
# 2.0		25/09/2025	Matthew Harman	Move to an incremental approach
# 3.0		28/10/2025	Matthew Harman	Added Deletes with incremental
# 4.0 		20/11/2025	Matthew Harman	Added in Parallelisation
#
# Configuration:	The following section should be modified to suit the
#			environment

# operation_mode		Operation mode, INCREMENTAL for incremental load
#						FULL for big bang
#				NOTE: CRIMINAL_JUSTICE_AREA will always be big 
#				bang as this does not have a CHANGED_DATE field
operation_mode='FULL';

# thread_count			Thread Count, how many streams do we run in
#				parallel, set to 1 to turn off parallelisation.
thread_count=2;

# spool_location		Location to store extracted files
spool_location='/opt/moj/rman/appreg';

# incremental_tracking_file	Location of file tracking incremental dates
#				for each table
incremental_tracking_file='/home/oracle/matt/appreg/incremental_tracker.txt';

# postgres_schema		The schema of the database in Postgres
postgres_schema='appreg_matt';

# postgres_schema_file		Location of the file created to reload the
#				postgres tables for staging the data
postgres_schema_file="${spool_location}/create_import_schema.sql";
# Blank the file
>${postgres_schema_file}

# postgres_delete_schema_file	Location of the file created to reload the
#				postgres tables for staging the deleted data
postgres_delete_schema_file="${spool_location}/create_delete_schema.sql";
# Blank the file
>${postgres_delete_schema_file}

# postgres_environment		Postgres environment connection string
#				NOTE: Don't put passwords here
postgres_environment='postgresql://pgadmin:<pwd>@appreg-stg.postgres.database.azure.com:5432/appreg-db';

# postgres_commands_file	Location of the file created to have the 
#				commands to load the .csv's into postgres
postgres_commands_file="${spool_location}/commands";

# postgres_delete_commands_file	Location of the file created to have the 
#				commands to load the .csv's into postgres
#				for the deleted records
postgres_delete_commands_file="${spool_location}/commands_delete.bat";
# Blank the file
>${postgres_delete_commands_file}

# postgres_insert_file		Location of the file created to insert the 
#				data into postgres
postgres_insert_file="${spool_location}/insert_data.sql";
# Blank the file
>${postgres_insert_file}

# postgres_delete_file		Location of the file created to delete the 
#				data into postgres
postgres_delete_file="${spool_location}/delete_data.sql";
# Blank the file
>${postgres_delete_file}

# Clear the deletes file
>deletes.sql

FIRST_TIME="YES";

BASE=1000;		# Base value used for lists in files, allows
			# max of 1000 tables.  Increment this is this 
			# becomes too small

for ((threads=0; threads<thread_count; threads++)); do
	>${postgres_commands_file}_part_${threads}.bat
done

# Set Arrays for tracking each paralel run
script_per_parallel=()
counts=()
max_idx=()
runs_list=""

export ORACLE_SID=LPRD1
export ORACLE_HOME=/opt/moj/oracle/product/10.2.0/db
export PATH=$ORACLE_HOME/bin:$PATH


SCN=$(sqlplus -s "/ as sysdba" <<END
col current_scn format 9999999999999
set feedback off
set head off
set pagesize 0
select ltrim(rtrim(current_scn)) from v\$database;
exit;
END
)

echo "scn is ${SCN}";

# Define functions
add_script_parallel() {
	local run="$1"
	local file="$2"

	# increment count for this run
	counts[$run]=$(( ${counts[$run]:-0} + 1 ))
	local idx="${counts[$run]}"
	local flat=$(( run * BASE + idx ))
echo "FLAT: ${flat}";
echo "file: ${file}";
	script_per_parallel[$flat]="${file}"
#	script_per_parallel[$flat]="hello world"
echo "script_per_parallel $flat = ${script_per_parallel[$flat]}";
}

pop_postgres5() {
	# Function to populate the sql_postgres5 variable
	# send debug to stderr so it doesn't contaminate the return
	# value
	local local_field_name=$1
	local local_conflict_field=$2
	local count_field=$3		# numeric
	local local_counter=$4		# numeric

	local return_string=""
	local line_sep="";

	if (( $count_field != $local_counter )); then
		line_sep=","
	fi

	# String compare for names
	if [[ "$local_field_name" != "$local_conflict_field" ]]; then
		return_string="${local_field_name} = EXCLUDED.${local_field_name}${line_sep}"
	fi
	
	printf '%s' "$return_string"
}
			

# TABLES_TO_EXTRACT	Stores a comma separated list of tables prefixed with
#			schema name that we need to migrate
# Removed APPREGISTER.DATA_AUDIT
# Keep the correct apply order so that there are no constraint violations
TABLES_TO_EXTRACT='APPREGISTER.APPLICATION_CODES,APPREGISTER.CRIMINAL_JUSTICE_AREA,APPREGISTER.APPLICATION_LISTS,APPREGISTER.STANDARD_APPLICANTS,APPREGISTER.NAME_ADDRESS,APPREGISTER.APPLICATION_LIST_ENTRIES,APPREGISTER.APPLICATION_REGISTER,APPREGISTER.FEE,APPREGISTER.APP_LIST_ENTRY_FEE_ID,APPREGISTER.APP_LIST_ENTRY_FEE_STATUS,APPREGISTER.APP_LIST_ENTRY_OFFICIAL,APPREGISTER.RESOLUTION_CODES,APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS,LIBRA.PETTY_SESSIONAL_AREAS,LIBRA.NATIONAL_COURT_HOUSES,LIBRA.LINK_ADDRESSES,LIBRA.ADDRESSES,LIBRA.COMMUNICATION_MEDIA,LIBRA.LINK_COMMUNICATION_MEDIA';
SEQUENCES_TO_EXTRACT='APPREGISTER.AC_SEQ,LIBRA.ADR_SEQ,APPREGISTER.ALEFS_SEQ,APPREGISTER.ALEO_SEQ,APPREGISTER.ALER_SEQ,APPREGISTER.AL_SEQ,APPREGISTER.AR_SEQ,APPREGISTER.CJA_SEQ,LIBRA.COMM_SEQ,APPREGISTER.FEE_SEQ,LIBRA.LA_SEQ,LIBRA.LCM_SEQ,APPREGISTER.NA_SEQ,LIBRA.PSA_SEQ,APPREGISTER.RC_SEQ,APPREGISTER.SA_SEQ';

# Table Fields		Each table has specific fields, detail them here
#		        NOTE: Add field type, postgres equivalent and whether 
#			NULL or NOT NULL
#			e.g. abc:VARCHAR:VARCHAR(10):N is a field called abc 
#			which is a VARCHAR, a VARCHAR(10) in Postgres  and 
#			nulls not allowed (NOT NULL)
APPLICATION_CODES_FIELDS='AC_ID:NUMBER:NUMERIC:N,APPLICATION_CODE:VARCHAR:VARCHAR(10):N,APPLICATION_CODE_TITLE:VARCHAR:VARCHAR(500):N,APPLICATION_CODE_WORDING:CLOB:TEXT:N,APPLICATION_LEGISLATION:CLOB:TEXT:Y,FEE_DUE:CHAR:CHAR(1):N,APPLICATION_CODE_RESPONDENT:CHAR:CHAR(1):N,AC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:VARCHAR(253):Y,AC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:VARCHAR(253):Y,APPLICATION_CODE_START_DATE:DATE:TEXT:N,APPLICATION_CODE_END_DATE:DATE:TEXT:Y,BULK_RESPONDENT_ALLOWED:CHAR:CHAR(1):N,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TEXT:N,USER_NAME:VARCHAR:VARCHAR(250):Y,AC_FEE_REFERENCE:VARCHAR:VARCHAR(12):Y';

APPLICATION_LISTS_FIELDS='AL_ID:NUMBER:NUMERIC:N,APPLICATION_LIST_STATUS:VARCHAR:VARCHAR(6):Y,APPLICATION_LIST_DATE:DATE:TIMESTAMP:N,APPLICATION_LIST_TIME:TIMESTAMP:TIMESTAMP:N,COURTHOUSE_CODE:VARCHAR:VARCHAR(10):Y,OTHER_COURTHOUSE:VARCHAR:VARCHAR(200):Y,LIST_DESCRIPTION:VARCHAR:VARCHAR(200):N,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y,COURTHOUSE_NAME:VARCHAR:VARCHAR(200):Y,DURATION_HOUR:NUMBER:SMALLINT:Y,DURATION_MINUTE:NUMBER:SMALLINT:Y,CJA_CJA_ID:NUMBER:NUMERIC:Y';

APPLICATION_LIST_ENTRIES_FIELDS='ALE_ID:NUMBER:NUMERIC:N,AL_AL_ID:NUMBER:NUMERIC:N,SA_SA_ID:NUMBER:NUMERIC:Y,AC_AC_ID:NUMBER:NUMERIC:N,A_NA_ID:NUMBER:NUMERIC:Y,R_NA_ID:NUMBER:NUMERIC:Y,NUMBER_OF_BULK_RESPONDENTS:NUMBER:SMALLINT:Y,APPLICATION_LIST_ENTRY_WORDING:CLOB:TEXT:N,CASE_REFERENCE:VARCHAR:VARCHAR(15):Y,ACCOUNT_NUMBER:VARCHAR:VARCHAR(20):Y,ENTRY_RESCHEDULED:CHAR:CHAR(1):N,NOTES:VARCHAR:VARCHAR(4000):Y,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMSTAMP:N,BULK_UPLOAD:VARCHAR:VARCHAR(1):Y,USER_NAME:VARCHAR:VARCHAR(250):Y,SEQUENCE_NUMBER:NUMBER:SMALLINT:N,TCEP_STATUS:VARCHAR:VARCHAR(2):Y,MESSAGE_UUID:VARCHAR:VARCHAR(36):Y,RETRY_COUNT:VARCHAR:VARCHAR(36):Y,LODGEMENT_DATE:DATE:TIMESTAMP:N';

APPLICATION_REGISTER_FIELDS='AR_ID:NUMBER:NUMERIC:N,AL_AL_ID:NUMBER:NUMERIC:N,TEXT:CLOB:TEXT:Y,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:TIMESTAMP:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y';

APP_LIST_ENTRY_FEE_ID_FIELDS='ALE_ALE_ID:NUMBER:NUMERIC:N,FEE_FEE_ID:NUMBER:NUMERIC:N,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):N';

APP_LIST_ENTRY_FEE_STATUS_FIELDS='ALEFS_ID:NUMBER:NUMERIC:N,ALEFS_ALE_ID:NUMBER:NUMERIC:N,ALEFS_PAYMENT_REFERENCE:VARCHAR:VARCHAR(15):Y,ALEFS_FEE_STATUS:VARCHAR:VARCHAR(1):N,ALEFS_FEE_STATUS_DATE:DATE:TIMESTAMP:N,ALEFS_VERSION:NUMBER:NUMERIC:N,ALEFS_CHANGED_BY:NUMBER:NUMERIC:N,ALEFS_CHANGED_DATE:DATE:TIMESTAMP:N,ALEFS_USER_NAME:VARCHAR:VARCHAR(250):N,ALEFS_STATUS_CREATION_DATE:DATE:TIMESTAMP:Y';

APP_LIST_ENTRY_OFFICIAL_FIELDS='ALEO_ID:NUMBER:NUMERIC:N,ALE_ALE_ID:NUMBER:NUMERIC:N,TITLE:VARCHAR:VARCHAR(100):Y,FORENAME:VARCHAR:VARCHAR(100):Y,SURNAME:VARCHAR:VARCHAR(100):Y,OFFICIAL_TYPE:VARCHAR:VARCHAR(1):N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):N';

APP_LIST_ENTRY_RESOLUTIONS_FIELDS='ALER_ID:NUMBER:NUMERIC:N,RC_RC_ID:NUMBER:NUMERIC:N,ALE_ALE_ID:NUMBER:NUMERIC:N,AL_ENTRY_RESOLUTION_WORDING:CLOB:TEXT:N,AL_ENTRY_RESOLUTION_OFFICER:VARCHAR:VARCHAR(1000):N,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y';

CRIMINAL_JUSTICE_AREA_FIELDS='CJA_ID:NUMBER:NUMERIC:N,CJA_CODE:VARCHAR:VARCHAR(2):N,CJA_DESCRIPTION:VARCHAR:VARCHAR(35):N';

DATA_AUDIT_FIELDS='DATA_ID:NUMBER:NUMERIC:N,SCHEMA_NAME:VARCHAR:VARCHAR(32):N,TABLE_NAME:VARCHAR:VARCHAR(32):N,COLUMN_NAME:VARCHAR:VARCHAR(32):N,OLD_VALUE:VARCHAR:VARCHAR(4000):Y,NEW_VALUE:VARCHAR:VARCHAR(4000):Y,USER_ID:VARCHAR:VARCHAR(32):Y,LINK:VARCHAR:VARCHAR(100):Y,CREATED_DATE:TIMESTAMP:TIMESTAMP:N,OLD_CLOB_VALUE:CLOB:TEXT:Y,NEW_CLOB_VALUE:CLOB:TEXT:Y,RELATED_KEY:NUMBER:NUMERIC:Y,UPDATE_TYPE:VARCHAR:VARCHAR(1):N,DATA_TYPE:VARCHAR:VARCHAR(1000):Y,CASE_ID:NUMBER:NUMERIC:Y,RELATED_ITEMS_IDENTIFIER:VARCHAR:VARCHAR(30):Y,RELATED_ITEMS_IDENTIFIER_INDEX:VARCHAR:VARCHAR(30):Y,EVENT_NAME:VARCHAR:VARCHAR(100):Y,USER_NAME:VARCHAR:VARCHAR(250):Y';

FEE_FIELDS='FEE_ID:NUMBER:NUMERIC:N,FEE_REFERENCE:VARCHAR:VARCHAR(12):N,FEE_DESCRIPTION:VARCHAR:VARCHAR(250):N,FEE_VALUE:NUMBER:DOUBLE PRECISION:N,FEE_START_DATE:DATE:TIMESTAMP:N,FEE_END_DATE:DATE:TIMESTAMP:Y,FEE_VERSION:NUMBER:NUMERIC:N,FEE_CHANGED_BY:NUMBER:NUMERIC:N,FEE_CHANGED_DATE:DATE:TIMESTAMP:N,FEE_USER_NAME:VARCHAR:VARCHAR(250):N';

NAME_ADDRESS_FIELDS='NA_ID:NUMBER:NUMERIC:N,CODE:VARCHAR:VARCHAR(10):Y,NAME:VARCHAR:VARCHAR(100):Y,TITLE:VARCHAR:VARCHAR(100):Y,FORENAME_1:VARCHAR:VARCHAR(100):Y,FORENAME_2:VARCHAR:VARCHAR(100):Y,FORENAME_3:VARCHAR:VARCHAR(100):Y,SURNAME:VARCHAR:VARCHAR(100):Y,ADDRESS_L1:VARCHAR:VARCHAR(35):N,ADDRESS_L2:VARCHAR:VARCHAR(35):Y,ADDRESS_L3:VARCHAR:VARCHAR(35):Y,ADDRESS_L4:VARCHAR:VARCHAR(35):Y,ADDRESS_L5:VARCHAR:VARCHAR(35):Y,POSTCODE:VARCHAR:VARCHAR(8):Y,EMAIL_ADDRESS:VARCHAR:VARCHAR(253):Y,TELEPHONE_NUMBER:VARCHAR:VARCHAR(20):Y,MOBILE_NUMBER:VARCHAR:VARCHAR(20):Y,VERSION:NUMBER:NUMERIC:Y,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y,DATE_OF_BIRTH:DATE:TIMESTAMP:Y,DMS_ID:VARCHAR:VARCHAR(20):Y';

RESOLUTION_CODES_FIELDS='RC_ID:NUMBER:NUMERIC:N,RESOLUTION_CODE:VARCHAR:VARCHAR(10):N,RESOLUTION_CODE_TITLE:VARCHAR:VARCHAR(500):N,RESOLUTION_CODE_WORDING:CLOB:TEXT:N,RESOLUTION_LEGISLATION:CLOB:TEXT:Y,RC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:VARCHAR(253):Y,RC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:VARCHAR(253):Y,RESOLUTION_CODE_START_DATE:DATE:TIMESTAMP:N,RESOLUTION_CODE_END_DATE:DATE:TIMESTAMP:Y,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y';

STANDARD_APPLICANTS_FIELDS='SA_ID:NUMBER:NUMERIC:N,STANDARD_APPLICANT_CODE:VARCHAR:VARCHAR(10):N,STANDARD_APPLICANT_START_DATE:DATE:TIMESTAMP:N,STANDARD_APPLICANT_END_DATE:DATE:TIMESTAMP:Y,VERSION:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:NUMERIC:N,CHANGED_DATE:DATE:TIMESTAMP:N,USER_NAME:VARCHAR:VARCHAR(250):Y,NAME:VARCHAR:VARCHAR(100):Y,TITLE:VARCHAR:VARCHAR(100):Y,FORENAME_1:VARCHAR:VARCHAR(100):Y,FORENAME_2:VARCHAR:VARCHAR(100):Y,FORENAME_3:VARCHAR:VARCHAR(100):Y,SURNAME:VARCHAR:VARCHAR(100):Y,ADDRESS_L1:VARCHAR:VARCHAR(35):N,ADDRESS_L2:VARCHAR:VARCHAR(35):Y,ADDRESS_L3:VARCHAR:VARCHAR(35):Y,ADDRESS_L4:VARCHAR:VARCHAR(35):Y,ADDRESS_L5:VARCHAR:VARCHAR(35):Y,POSTCODE:VARCHAR:VARCHAR(8):Y,EMAIL_ADDRESS:VARCHAR:VARCHAR(253):Y,TELEPHONE_NUMBER:VARCHAR:VARCHAR(20):Y,MOBILE_NUMBER:VARCHAR:VARCHAR(20):Y';

NATIONAL_COURT_HOUSES_FIELDS='NCH_ID:NUMBER:BIGINT:N,COURTHOUSE_NAME:VARCHAR:VARCHAR(100):N,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N,COURT_TYPE:VARCHAR:VARCHAR(10):N,START_DATE:DATE:TIMESTAMP:N,END_DATE:DATE:TIMESTAMP:Y,LOC_LOC_ID:NUMBER:BIGINT:Y,PSA_PSA_ID:NUMBER:BIGINT:Y,COURT_LOCATION_CODE:VARCHAR:VARCHAR(10):Y,SL_COURTHOUSE_NAME:VARCHAR:VARCHAR(100):Y,NORG_ID:NUMBER:BIGINT:Y';

LINK_ADDRESSES_FIELDS='LA_ID:NUMBER:BIGINT:N,NO_FIXED_ABODE:VARCHAR:VARCHAR(1):N,LA_TYPE:VARCHAR:VARCHAR(5):N,START_DATE:DATE:TIMESTAMP:N,END_DATE:DATE:TIMESTAMP:Y,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N,ADR_ADR_ID:NUMBER:BIGINT:N,BU_BU_ID:NUMBER:BIGINT:N,ER_ER_ID:NUMBER:BIGINT:Y,LOC_LOC_ID:NUMBER:BIGINT:Y,HEAD_OFFICE_INDICATOR:VARCHAR:VARCHAR(1):Y';

ADDRESSES_FIELDS='ADR_ID:NUMBER:BIGINT:N,LINE1:VARCHAR:VARCHAR(35):Y,LINE2:VARCHAR:VARCHAR(35):Y,LINE3:VARCHAR:VARCHAR(35):Y,LINE4:VARCHAR:VARCHAR(35):Y,LINE5:VARCHAR:VARCHAR(35):Y,POSTCODE:VARCHAR:VARCHAR(8):Y,START_DATE:DATE:TIMESTAMP:N,END_DATE:DATE:TIMESTAMP:Y,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N,MCC_MCC_ID:NUMBER:BIGINT:Y';

LINK_COMMUNICATION_MEDIA_FIELDS='LCM_ID:NUMBER:BIGINT:N,LCM_TYPE:VARCHAR:VARCHAR(2):N,START_DATE:DATE:TIMESTAMP:Y,END_DATE:DATE:TIMESTAMP:Y,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N,COMM_COMM_ID:NUMBER:BIGINT:N,LOC_LOC_ID:NUMBER:BIGINT:Y,ER_ER_ID:NUMBER:BIGINT:Y,BU_BU_ID:NUMBER:BIGINT:Y';

COMMUNICATION_MEDIA_FIELDS='COMM_ID:NUMBER:BIGINT:N,DETAIL:VARCHAR:VARCHAR(254):N,START_DATE:DATE:TIMESTAMP:N,END_DATE:DATE:TIMESTAMP:Y,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N';

PETTY_SESSIONAL_AREAS_FIELDS='PSA_ID:NUMBER:BIGINT:N,PSA_NAME:VARCHAR:VARCHAR(100):Y,SHORT_NAME:VARCHAR:VARCHAR(10):Y,VERSION_NUMBER:NUMBER:NUMERIC:N,CHANGED_BY:NUMBER:BIGINT:N,CHANGED_DATE:DATE:TIMESTAMP:N,CMA_CMA_ID:NUMBER:BIGINT:Y,PSA_CODE:VARCHAR:VARCHAR(4):N,START_DATE:DATE:TIMESTAMP:Y,END_DATE:DATE:TIMESTAMP:Y,JC_NAME:VARCHAR:VARCHAR(200):Y,COURT_TYPE:VARCHAR:VARCHAR(10):N,CRIME_CASES_LOC_ID:NUMBER:BIGINT:Y,FINE_ACCOUNTS_LOC_ID:NUMBER:BIGINT:Y,MAINTENANCE_ENFORCEMENT_LOC_ID:NUMBER:BIGINT:Y,FAMILY_CASES_LOC_ID:NUMBER:BIGINT:Y,COURT_LOCATION_CODE:VARCHAR:VARCHAR(10):Y,CENTRAL_FINANCE_LOC_ID:NUMBER:BIGINT:Y,SL_PSA_NAME:VARCHAR:VARCHAR(100):Y,NORG_ID:NUMBER:BIGINT:Y';

# Further configuration that should not need changing
sql_header1="SET PAGESIZE 0 HEADING OFF FEEDBACK OFF VERIFY OFF";
sql_header2="SET LONG 1000000000 LONGCHUNKSIZE 10000";
sql_header3="SET LINESIZE 10000 TRIMSPOOL OFF TAB OFF TERMOUT OFF ECHO OFF";

sql_header_seq1="SET PAGESIZE 0 HEADING OFF FEEDBACK OFF VERIFY OFF";
sql_header_seq2="SET LONG 1000000000 LONGCHUNKSIZE 10000";
sql_header_seq3="SET LINESIZE 300 TRIMSPOOL OFF TAB OFF TERMOUT OFF ECHO OFF";

# Main Code
calling_script="";
FIELD_SEPARATOR=$IFS
IFS=','
NEWLINE=$'\n'

DATE_TIME=`date +%Y%m%d-%H%M`

if [ $operation_mode == "INCREMENTAL" ]
then
	if [ ! -f $incremental_tracking_file ]
	then
		>$incremental_tracking_file
	else
		# backup the file
		mv $incremental_tracking_file ${incremental_tracking_file}_${DATE_TIME}	
		FIRST_TIME="NO";
	fi
	now_date=`date +%Y-%m-%d`
	now_time=`date +%H:%M:%S`
fi

# Loop through the TABLES
for tables_to_extract in $TABLES_TO_EXTRACT
do
	sql_head="";
	sql_spool="";
	sql_script="";
	echo "starting extracting $tables_to_extract at `date`"
	calling_script="${calling_script}@${tables_to_extract}.sql${NEWLINE}";
echo "running case: $tables_to_extract $lower_table_name";
	case $tables_to_extract in
		APPREGISTER.APPLICATION_CODES)
			echo "in APPLICATION_CODES"
			table_fields=$APPLICATION_CODES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			conflict_field="AC_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			incremental_allowed="YES";
			lower_table_name='application_codes';
			lower_with_schema='appregister.application_codes';
			changed_date='changed_date';
			field_count=17;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="AC_ID";
			;;
		APPREGISTER.APPLICATION_LISTS)
			echo "in APPLICATION_LISTS"
			table_fields=$APPLICATION_LISTS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="AL_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='application_lists';
			lower_with_schema='appregister.application_lists';
			changed_date='changed_date';
			field_count=15;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="AL_ID";
			;;
		APPREGISTER.APPLICATION_LIST_ENTRIES)
			echo "in APPLICATION_LIST_ENTRIES"
			table_fields=$APPLICATION_LIST_ENTRIES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="ALE_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='application_list_entries';
			lower_with_schema='appregister.application_list_entries';
			changed_date='changed_date';
			field_count=22;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="ALE_ID";
			;;
		APPREGISTER.APPLICATION_REGISTER)
			echo "in APPLICATION_REGISTER"
			table_fields=$APPLICATION_REGISTER_FIELDS;
			split_lob_into_chunks="YES";
			order_by_field="AR_ID";
			conflict_field="AR_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='application_register';
			lower_with_schema='appregister.application_register';
			incremental_allowed="YES";
			changed_date='changed_date';
			field_count=6;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="AR_ID";
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_ID)
			echo "in APP_LIST_ENTRY_FEE_ID"
			table_fields=$APP_LIST_ENTRY_FEE_ID_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			conflict_field="";
			conflict_constraint="YES";
			conflict_constraint_name='ux_app_list_entry_fee_id_row';
			incremental_allowed="YES";
			lower_table_name='app_list_entry_fee_id';
			lower_with_schema='appregister.app_list_entry_fee_id';
			changed_date='changed_date';
			field_count=6;
			use_hash="YES";
			hash_index="CREATE UNIQUE INDEX IF NOT EXISTS ux_app_list_entry_fee_id_row_idx ON ${postgres_schema}.app_list_entry_fee_id(ale_ale_id,fee_fee_id,version,changed_by,changed_date,user_name);";
			hash_index_constraint="ALTER TABLE ${postgres_schema}.app_list_entry_fee_id ADD CONSTRAINT ux_app_list_entry_fee_id_row UNIQUE USING INDEX ux_app_list_entry_fee_id_row_idx;";
			hash_index_drop="alter table ${postgres_schema}.app_list_entry_fee_id drop constraint ux_app_list_entry_fee_id_row;";
			shard_field="ALE_ALE_ID";
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_STATUS)
			echo "in APP_LIST_ENTRY_FEE_STATUS"
			table_fields=$APP_LIST_ENTRY_FEE_STATUS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="ALEFS_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='app_list_entry_fee_status';
			lower_with_schema='appregister.app_list_entry_fee_status';
			changed_date='alefs_changed_date';
			field_count=10;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="ALEFS_ID";
			;;
		APPREGISTER.APP_LIST_ENTRY_OFFICIAL)
			echo "in APP_LIST_ENTRY_OFFICIAL"
			table_fields=$APP_LIST_ENTRY_OFFICIAL_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="ALEO_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='app_list_entry_official';
			lower_with_schema='appregister.app_list_entry_official';
			changed_date='changed_date';
			field_count=9;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="ALEO_ID";
			;;
		APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS)
			echo "in APP_LIST_ENTRY_RESOLUTIONS"
			table_fields=$APP_LIST_ENTRY_RESOLUTIONS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="ALER_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='app_list_entry_resolutions';
			lower_with_schema='appregister.app_list_entry_resolutions';
			changed_date='changed_date';
			field_count=9;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="ALER_ID";
			;;
		APPREGISTER.CRIMINAL_JUSTICE_AREA)
			echo "in CRIMINAL_JUSTICE_AREA"
			table_fields=$CRIMINAL_JUSTICE_AREA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="NO";
			conflict_field="CJA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='criminal_justice_area';
			lower_with_schema='appregister.criminal_justice_area';
			field_count=3;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			drop_constraint="alter table ${postgres_schema}.application_lists drop constraint al_cja_fk;";
			create_constraint="alter table ${postgres_schema}.application_lists add constraint al_cja_fk foreign key (cja_cja_id) references ${postgres_schema}.criminal_justice_area(cja_id) on delete no action not deferrable initially immediate;";
			shard_field="CJA_ID";
			;;
		APPREGISTER.DATA_AUDIT)
			echo "in DATA_AUDIT"
			table_fields=$DATA_AUDIT_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="DATA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='data_audit';
			lower_with_schema='appregister.data_audit';
			changed_date='created_date';
			field_count=19;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="DATA_ID";
			;;
		APPREGISTER.FEE)
			echo "in FEE"
			table_fields=$FEE_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="FEE_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='fee';
			lower_with_schema='appregister.fee';
			changed_date='fee_changed_date';
			field_count=10;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="FEE_ID";
			;;
		APPREGISTER.NAME_ADDRESS)
			echo "in NAME_ADDRESS"
			table_fields=$NAME_ADDRESS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="NA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='name_address';
			lower_with_schema='appregister.name_address';
			changed_date='changed_date';
			field_count=23;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="NA_ID";
			;;
		APPREGISTER.RESOLUTION_CODES)
			echo "in RESOLUTION_CODES"
			table_fields=$RESOLUTION_CODES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="RC_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='resolution_codes';
			lower_with_schema='appregister.resolution_codes';
			changed_date='changed_date';
			field_count=13;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="RC_ID";
			;;
		APPREGISTER.STANDARD_APPLICANTS)
			echo "in STANDARD_APPLICANTS"
			table_fields=$STANDARD_APPLICANTS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="SA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='standard_applicants';
			lower_with_schema='appregister.standard_applicants';
			changed_date='changed_date';
			field_count=23;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="SA_ID";
			;;
		LIBRA.NATIONAL_COURT_HOUSES)
			echo "in NATIONAL_COURT_HOUSES"
			table_fields=$NATIONAL_COURT_HOUSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="NCH_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='national_court_houses';
			lower_with_schema='libra.national_court_houses';
			changed_date='changed_date';
			field_count=13;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="NCH_ID";
			;;
		LIBRA.LINK_ADDRESSES)
			echo "in LINK_ADDRESSES"
			table_fields=$LINK_ADDRESSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="LA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='link_addresses';
			lower_with_schema='libra.link_addresses';
			changed_date='changed_date';
			field_count=13;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="LA_ID";
			;;
		LIBRA.ADDRESSES)
			echo "in ADDRESSES"
			table_fields=$ADDRESSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="ADR_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='addresses';
			lower_with_schema='libra.addresses';
			changed_date='changed_date';
			field_count=13;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="ADR_ID";
			;;
		LIBRA.LINK_COMMUNICATION_MEDIA)
			echo "in LINK_COMMUNICATION_MEDIA"
			table_fields=$LINK_COMMUNICATION_MEDIA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="LCM_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='link_communication_media';
			lower_with_schema='libra.link_communication_media';
			changed_date='changed_date';
			field_count=11;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="LCM_ID";
			;;
		LIBRA.COMMUNICATION_MEDIA)
			echo "in COMMUNICATION_MEDIA"
			table_fields=$COMMUNICATION_MEDIA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="COMM_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='communication_media';
			lower_with_schema='libra.communication_media';
			changed_date='changed_date';
			field_count=7;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="COMM_ID";
			;;
		LIBRA.PETTY_SESSIONAL_AREAS)
			echo "in PETTY_SESSIONAL_AREAS"
			table_fields=$PETTY_SESSIONAL_AREAS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			incremental_allowed="YES";
			conflict_field="PSA_ID";
			conflict_constraint="NO";
			conflict_constraint_name='';
			lower_table_name='petty_sessional_areas';
			lower_with_schema='libra.petty_sessional_areas';
			changed_date='changed_date';
			field_count=20;
			use_hash="NO";
			hash_index='';
			hash_index_constraint='';
			hash_index_drop='';
			shard_field="PSA_ID";
			;;
	esac

	# Need to loop through the fields
	if [[ ${split_lob_into_chunks} == "NO" ]]
	then
		# not multi chunk 
		echo "table is not a multi chunk one";
		l_have_where="NO";
		
		# Populate the drop statement into the postgres_schema_file
		echo "DROP TABLE IF EXISTS ${postgres_schema}.${lower_table_name}_temp;${NEWLINE}">>${postgres_schema_file};
		echo "${NEWLINE}">>${postgres_schema_file};
		echo "${hash_index}${NEWLINE}">>${postgres_schema_file};
		echo "${hash_index_constraint}${NEWLINE}">>${postgres_schema_file};

		# If we are doing incremental and incremental not allowed
		# on table, then delete the existing target data as this is
		# a full table reload
		if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "NO" ]
		then
			echo "${drop_constraint}${NEWLINE}">>${postgres_insert_file};
			echo "delete from ${postgres_schema}.${lower_table_name};${NEWLINE}">>${postgres_insert_file};

		fi

		# Generate the sql script
		sql_head="${sql_header1}${NEWLINE}${sql_header2}";
		sql_head="${sql_head}${NEWLINE}${sql_header3}${NEWLINE}";

		# And the postgres script
		sql_postgres="WITH cleaned AS (${NEWLINE}";
		sql_postgres="${sql_postgres}SELECT${NEWLINE}";
			
		sql_postgres2="SELECT${NEWLINE}";
		sql_postgres3="INSERT INTO ${postgres_schema}.${lower_table_name} AS t (${NEWLINE}";
		sql_postgres4="SELECT${NEWLINE}";
		sql_postgres5="";

echo "sql: $sql_script"
		
echo "LLLLL: ${sql_head}"
		sql_script="${NEWLINE}SELECT${NEWLINE}";
echo "sql2: $sql_script"

		# How many fields are in this table
		#field_count=`echo $table_fields|wc -w`
		counter=0;

		# Do the create table line
		echo "CREATE UNLOGGED TABLE IF NOT EXISTS ${postgres_schema}.${lower_table_name}_temp (">>${postgres_schema_file};

		# Loop through the fields
		for field_info in $table_fields
		do
			counter=`echo $counter+1|bc`;

			# We need to split the field_info into its 4 components
			field_name=`echo ${field_info}|awk -F":" '{print $1}'`
			lower_field_name=`echo ${field_name}|tr '[:upper:]' '[:lower:]'`
			field_type=`echo ${field_info}|awk -F":" '{print $2}'`
			postgres_field_type=`echo ${field_info}|awk -F":" '{print $3}'`
			field_nullable=`echo ${field_info}|awk -F":" '{print $4}'`

			case $field_type in
				NUMBER)
					echo "field is a number";
					if [[ $field_nullable == "Y" ]]
					then
echo "is nulls"
						sql_script="${sql_script}TO_CLOB(NVL(TO_CHAR(${field_name}),''))"
					else
echo "is notnull"
						sql_script="${sql_script}TO_CLOB(TO_CHAR(${field_name}))"
					fi
			
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						sql_postgres4="${sql_postgres4}${field_name}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						sql_postgres4="${sql_postgres4}${field_name},${NEWLINE}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				VARCHAR)
					echo "field is a varchar";
					# we will also need to strip any broken bars in
					# the test data
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(NVL(${field_name},'')),UNISTR('\00A6')),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					else
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(${field_name}),UNISTR('\00A6')),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					fi
					field_size=`echo ${postgres_field_type}|awk -F"(" '{print $2}'`
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(left(${field_name}, ${field_size},'')";
						else
							sql_postgres4="${sql_postgres4}left(${field_name}, ${field_size}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(left(${field_name}, ${field_size},''),${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}left(${field_name}, ${field_size},${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				CLOB)
echo "field_nullable: $field_nullable";
					echo "field is a clob";
					if [[ $field_nullable == "Y" ]]
					#if [[ $field_nullable == "Y" ]]
					then
echo "field is NULL"
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(NVL(${field_name},'')),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					else
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(${field_name}),'\','\\\\'),'|','\p'),CHR(14),'\r'),CHR(10),'\n'),CHR(9),'\t')"
echo "NOTNULL field"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres3="${sql_postgres3}${field_name}";
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},'')";
						else
							sql_postgres3="${sql_postgres3}${field_name}";
							sql_postgres4="${sql_postgres4}${field_name}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
echo "aa"
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name},${NEWLINE}";
echo "bb"
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name},${NEWLINE}";
echo "cc"
						if [[ $field_nullable == "Y" ]]
						then
echo "cca"
							sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},''),${NEWLINE}";
						else
echo "ccb"
							sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
							sql_postgres4="${sql_postgres4}${field_name},${NEWLINE}";
						fi
echo "dd"
echo "sql_postgres5: ${sql_postgres5}"
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
echo "ee"
echo "sql_postgres5: ${sql_postgres5}"
					fi
					;;
				CHAR)
					echo "field is a char";
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(NVL((${field_name},'')),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					else
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(${field_name}),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					fi
					field_size=`echo ${postgres_field_type}|awk -F"(" '{print $2}'`
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						sql_postgres4="${sql_postgres4}left(${field_name},${field_size}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}REPLACE(${field_name},'\\\\','\') AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						sql_postgres4="${sql_postgres4}left(${field_name},${field_size},${NEWLINE}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				DATE)
					echo "field is a date";
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}TO_CLOB(NVL(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS'),''))"
					else
						sql_script="${sql_script}TO_CLOB(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS'))"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},'')::timestamp";
						else
							sql_postgres4="${sql_postgres4}(${field_name})::timestamp";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},'')::timestamp,${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}(${field_name})::timestamp,${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				TIMESTAMP)
					echo "field is a timestamp";
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}TO_CLOB(NVL(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS.FF6'),''))"
					else
						sql_script="${sql_script}TO_CLOB(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS.FF6'))"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}${field_name}${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},'')::timestamp";
						else
							sql_postgres4="${sql_postgres4}(${field_name})::timestamp";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}NULLIF(${field_name},'')::timestamp,${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}(${field_name})::timestamp,${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
			
			esac
echo "checking: ${field_name} ${counter} ${field_count}";

			if [[ $counter -lt $field_count ]]
			then
echo "a1";
				sql_script="${sql_script}||'|'||${NEWLINE}";
			else
echo "a2";
				sql_script="${sql_script}||'|#'${NEWLINE}";
			fi
echo "end checking";
echo "sqlaa: $sql_script";
	

			
			# Write out the postgres create schema file
			if [[ ${field_type} == "NUMBER" ]]
			then
				echo "${lower_field_name} NUMERIC,">>${postgres_schema_file};
			else
				echo "${lower_field_name} TEXT,">>${postgres_schema_file};
			fi
			
			field_nullable='';

		done

		sql_postgres="${sql_postgres}FROM ${postgres_schema}.${lower_table_name}_temp${NEWLINE}";
		sql_postgres="${sql_postgres}),${NEWLINE}";
		sql_postgres="${sql_postgres}backslashes_fixed AS (";

		# Add the marker field
		echo "marker text">>${postgres_schema_file}
		echo ");${NEWLINE}">>${postgres_schema_file};

echo "sql3: $sql_script"
		sql_script="${sql_script}FROM ${tables_to_extract} AS OF SCN ${SCN}${NEWLINE}";
echo "AA: ${l_have_where}";
		if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "YES" ]
		then
			if [[ ${l_have_where} == "NO" ]] 
			then
				sql_script="${sql_script}WHERE${NEWLINE}";
			else
				sql_script="${sql_script}WHERE${NEWLINE}";
			fi
			l_have_where="YES";
echo "AB: ${l_have_where}";
			# Need to write the date filter
			# Have we done this table before
			record_count=`cat ${incremental_tracking_file}_${DATE_TIME}|grep ${tables_to_extract}|wc -l`;
			if [ $record_count -eq 1 ]
			then
				# extract the lower water mark
echo "before lwm"
echo "tables to extract ${tables_to_extract}";
echo "tracking file: ${incremental_tracking_file}";
				lwm_date="$(grep "^${tables_to_extract}#" "${incremental_tracking_file}_${DATE_TIME}" | awk -F'#' '{print $2}')"
				lwm_time="$(grep "^${tables_to_extract}#" "${incremental_tracking_file}_${DATE_TIME}" | awk -F'#' '{print $3}')"
echo "lwm_date: ${lwm_date}"
echo "lwm_time: ${lwm_time}"
				sql_script="${sql_script}FROM_TZ(CAST(${changed_date} AS TIMESTAMP), DBTIMEZONE) AT TIME ZONE 'UTC' > TO_TIMESTAMP_TZ('${lwm_date} ${lwm_time} UTC', 'YYYY-MM-DD HH24:MI:SS TZR')${NEWLINE}";
				sql_script="${sql_script}AND${NEWLINE}";
echo "${sql_script}";
			fi
			sql_script="${sql_script}FROM_TZ(CAST(${changed_date} AS TIMESTAMP), DBTIMEZONE) AT TIME ZONE 'UTC' <= TO_TIMESTAMP_TZ('${now_date} ${now_time} UTC', 'YYYY-MM-DD HH24:MI:SS TZR')${NEWLINE}";
echo "ZZ: ${sql_script}";
		fi

		# Shard the data
		sql_script_base="${sql_script}";
		adjusted_thread_count=$((thread_count -1));
echo "AAAAA ${thread_count}";
		for ((threads=0; threads<thread_count; threads++)); do
echo "BBBBB";
echo "${threads}";
			# Populate the postgres commands file
			echo "\"c:\Program Files\PostgreSQL\16\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy ${postgres_schema}.${lower_table_name}_temp FROM '${lower_with_schema}_part_${threads}.csv' WITH (FORMAT text, DELIMITER '|', NULL '')\" \"${postgres_environment}\"">>${postgres_commands_file}_part_${threads}.bat;

			sql_spool="spool ${spool_location}/${tables_to_extract}_part_${threads}.csv;";
			sql_script=$sql_script_base;
echo "AC: ${l_have_where}";
			if [[ ${l_have_where} == "NO" ]] || [[ $incremental_allowed == "NO" ]] || [[ ${operation_mode} == "FULL" ]]
			then
echo "AD: ${l_have_where}";
				sql_script="${sql_script}WHERE (ORA_HASH(TO_CHAR(${shard_field}), ${adjusted_thread_count-1}) = ${threads})${NEWLINE}";
			else
echo "AE: ${l_have_where}";
				sql_script="${sql_script}AND (ORA_HASH(TO_CHAR(${shard_field}), ${adjusted_thread_count-1}) = ${threads})${NEWLINE}";
			fi
			l_have_where="YES";

			if [[ ${order_by_field} -ne "" ]]
			then
				sql_script="${sql_script}ORDER BY ${order_by_field};";
			else
				sql_script="${sql_script};";
			fi
			sql_script="${sql_script}${NEWLINE}spool off;${NEWLINE}";

			# Write the script to a file
			echo "${sql_head}">${tables_to_extract}_part_${threads}.sql;
			echo "${sql_spool}">>${tables_to_extract}_part_${threads}.sql;
			echo "${sql_script}">>${tables_to_extract}_part_${threads}.sql;
echo 'ADDING $threads "@${tables_to_extract}_part_${threads}.sql${NEWLINE"';
			add_script_parallel $threads "@${tables_to_extract}_part_${threads}.sql${NEWLINE}";
echo "ADDING COMPLETE";
echo "CCCCC";
		done
echo "DDDDD";
	
		# Write the postgres to a file
		echo "${sql_postgres}">>$postgres_insert_file;
		echo "${sql_postgres2}">>$postgres_insert_file;
		echo "FROM cleaned">>$postgres_insert_file;
		echo ")">>$postgres_insert_file;
		echo "${sql_postgres3}">>$postgres_insert_file;	
		echo ")">>$postgres_insert_file;
		echo "${sql_postgres4}">>$postgres_insert_file;
		echo "FROM backslashes_fixed">>$postgres_insert_file;
		if [[ ${conflict_constraint} == "NO" ]]
		then
			echo "ON CONFLICT (${conflict_field}) DO UPDATE SET">>$postgres_insert_file;
		else
			echo "ON CONFLICT ON CONSTRAINT ${conflict_constraint_name} DO UPDATE SET">>$postgres_insert_file;
		fi
		echo "${sql_postgres5}">>$postgres_insert_file;
		echo ";${NEWLINE}">>$postgres_insert_file;

		if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "YES" ]
		then
			# record the hwm in the tracking file
			echo "${tables_to_extract}#${now_date}#${now_time}#${SCN}">>${incremental_tracking_file};
		fi
		echo "${hash_index_drop}${NEWLINE}">>${postgres_insert_file};

		# If incremental and not allowed on the table, we need to 
		# recreate any constraints dropped earlier
		if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "NO" ]
		then
			echo "${create_constraint}${NEWLINE}">>${postgres_insert_file};

		fi
echo "sql4: $sql_script"
	else
		# multi chunk 
		l_have_where="NO";
		echo "table is a multi chunk one";
echo "MULTI:"
echo "head: ${sql_head}";
echo "spool: ${sql_spool}";
echo "script: ${sql_script}";
echo "ITLUM"


		# Populate the drop statement into the postgres_schema_file
		echo "DROP TABLE IF EXISTS ${postgres_schema}.${lower_table_name}_temp;${NEWLINE}">>${postgres_schema_file};
		echo "${NEWLINE}">>${postgres_schema_file};
		echo "${hash_index}${NEWLINE}">>${postgres_schema_file};
		echo "${hash_index_constraint}${NEWLINE}">>${postgres_schema_file};

		# Generate the sql script
		sql_head="${sql_header1}${NEWLINE}${sql_header2}";
		sql_head="${sql_head}${NEWLINE}${sql_header3}${NEWLINE}";

echo "FINISHED HEAD: ${sql_head}";
echo "HEAD"
		# And the postgres script
		sql_postgres="WITH unescaped AS (${NEWLINE}";
		sql_postgres="${sql_postgres}SELECT${NEWLINE}";

		sql_postgres2="reconstructed AS (${NEWLINE}";
		sql_postgres2="${sql_postgres2}SELECT${NEWLINE}";
		sql_postgres3="INSERT INTO ${postgres_schema}.${lower_table_name} AS t (${NEWLINE}";
		sql_postgres4="SELECT${NEWLINE}";
		sql_postgres5="";

		sql_script="${sql_script}${NEWLINE}WITH base AS (${NEWLINE}";
		sql_script="${sql_script}SELECT${NEWLINE}";
echo "sql2: $sql_script"

		# How many fields are in this table
		#field_count=`echo $table_fields|wc -w`
		counter=0;

		# Do the create table line
		echo "CREATE UNLOGGED TABLE IF NOT EXISTS ${postgres_schema}.${lower_table_name}_temp (">>${postgres_schema_file};

		sql1_script="";
		sql2_script="";

		# Loop through the fields
		for field_info in $table_fields
		do
			counter=`echo $counter+1|bc`;

			# We need to split the field_info into its 4 components
			field_name=`echo ${field_info}|awk -F":" '{print $1}'`
echo "field_name: ${field_name}"
			lower_field_name=`echo ${field_name}|tr '[:upper:]' '[:lower:]'`
echo "lower_field_name: ${lower_field_name}"
			field_type=`echo ${field_info}|awk -F":" '{print $2}'`
			postgres_field_type=`echo ${field_info}|awk -F":" '{print $3}'`
			field_nullable=`echo ${field_info}|awk -F":" '{print $4}'`
			case $field_type in
				NUMBER)
					echo "field is a number ${field_name}";
					sql1_script="${sql1_script}${field_name},${NEWLINE}";
					if [[ $field_nullable == "Y" ]]
					then
echo "is nulls"
						sql2_script2="${sql2_script}TO_CLOB(NVL(TO_CHAR(b.${field_name}),''))"
					else
echo "is notnull"
						sql2_script="${sql2_script}TO_CLOB(TO_CHAR(b.${field_name}))"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}${field_name}";
						if [[ ${conflict_field} -eq ${field_name} ]]
						then
							sql_postgres2="${sql_postgres2}${field_name}";
						else
							sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name}";
						fi
						sql_postgres3="${sql_postgres3}${field_name}";
						sql_postgres4="${sql_postgres4}r.${field_name}${NEWLINE}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}${field_name},${NEWLINE}";
echo "conflict: ${conflict_field}";
echo "field_name: ${field_name}";

						if [[ ${conflict_field} == ${field_name} ]]
						then
echo "zz"
							sql_postgres2="${sql_postgres2}${field_name},${NEWLINE}";
echo "yy"
						else
echo "xx"
							sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name},${NEWLINE}";
echo "vv"
						fi
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				VARCHAR)
					echo "field is a varchar ${field_name}";
					sql1_script="${sql1_script}${field_name},${NEWLINE}";
					if [[ $field_nullable == "Y" ]]
					then
						sql2_script="${sql2_script}TO_CLOB(NVL(b.${field_name},''))"
					else
						sql2_script="${sql2_script}TO_CLOB(b.${field_name})"
					fi
					field_size=`echo ${postgres_field_type}|awk -F"(" '{print $2}'`
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}NULLIF(${field_name},'')::varchar(${field_size} AS ${field_name}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}NULLIF(${field_name},'')::varchar(${field_size} AS ${field_name},${NEWLINE}";
#						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n') AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name}${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				CLOB)
echo "field_nullable: $field_nullable";
					echo "field is a clob ${field_name}";
					l_clob_string="REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(DBMS_LOB.SUBSTR(b.${field_name},3900,1+(s.piece_no-1)*3900)),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')||'|#|'"
echo "NOTNULL field"
					l_clob_field=${field_name};
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}piece_no,${NEWLINE}";
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS piece";
						sql_postgres2="${sql_postgres2}string_agg(piece, '' ORDER BY piece_no) AS text_full";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres3="${sql_postgres3}${field_name}";
							sql_postgres4="${sql_postgres4}NULLIF(r.${field_name}_full,'') AS ${field_name}";
						else
							sql_postgres3="${sql_postgres3}${field_name}${NEWLINE}";
							sql_postgres4="${sql_postgres4}NULLIF(r.${field_name}_full,'') AS ${field_name}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}piece_no,${NEWLINE}";
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS piece,${NEWLINE}";
						sql_postgres2="${sql_postgres2}string_agg(piece, '' ORDER BY piece_no) AS text_full,${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
							sql_postgres4="${sql_postgres4}NULLIF(r.${field_name}_full,'') AS ${field_name},${NEWLINE}";
						else
							sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
							sql_postgres4="${sql_postgres4}NULLIF(r.${field_name}_full,'') AS ${field_name},${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
echo "sql_string123: ${sql_script}";
					sql2_script="${sql2_script}${l_clob_string}||${NEWLINE}";
					sql1_script="${sql1_script}${field_name},${NEWLINE}";
echo "sql_string456: ${sql_script}";
					;;
				CHAR)
					echo "field is a char ${field_name}";
					sql1_script="${sql1_script}${field_name},${NEWLINE}";
					if [[ $field_nullable == "Y" ]]
					then
						sql2_script="${sql2_script}TO_CLOB(NVL((b.${field_name},''))"
					else
						sql2_script="${sql2_script}TO_CLOB(${b.field_name})"
					fi
					field_size=`echo ${postgres_field_type}|awk -F"(" '{print $2}'`
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS ${field_name}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						sql_postgres4="${sql_postgres4}r.${field_name}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				DATE)
echo $field_name
					echo "field is a date: ${field_name}";
					sql1_script="${sql1_script}($field_name AT TIME ZONE 'UTC') AS ${field_name}_utc,${NEWLINE}";
					if [[ $field_nullable == "Y" ]]
					then
						sql2_script="${sql2_script}TO_CLOB(NVL(TO_CHAR(b.${field_name}_utc,'YYYY-MM-DD\"T\"HH24:MI:SS.FF6\"Z\"')),'')"
					else
						sql2_script="${sql2_script}TO_CLOB(TO_CHAR(b.${field_name}_utc,'YYYY-MM-DD\"T\"HH24:MI:SS.FF6\"Z\"'))"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS ${field_name}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(${field_name},'\p','|'), '\t',E'\t'), '\r', E'\r'), '\n', E'\n'), '\\\\', '\') AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				TIMESTAMP)
					echo "field is a timestamp ${field_name}";
					sql1_script="${sql1_script}(${field_name} AT TIME ZONE 'UTC') AS ${field_name}_utc,${NEWLINE}";
					if [[ $field_nullable == "Y" ]]
					then
						sql2_script="${sql2_script}TO_CLOB(NVL(TO_CHAR(b.${field_name}_utc,'YYYY-MM-DD\"T\"HH24:MI:SS.FF6\"Z\"')),'')"
					else
						sql2_script="${sql2_script}TO_CLOB(TO_CHAR(b.${field_name}_utc,'YYYY-MM-DD\"T\"HH24:MI:SS.FF6\"Z\"'))"
					fi
					if [[ $field_count -eq $counter ]]
					then
						sql_postgres="${sql_postgres}(${field_name})::timestamptz AS ${field_name}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name}";
						sql_postgres3="${sql_postgres3}${field_name}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					else
						sql_postgres="${sql_postgres}(${field_name})::timestamptz AS ${field_name},${NEWLINE}";
						sql_postgres2="${sql_postgres2}MIN(${field_name}) AS ${field_name},${NEWLINE}";
						sql_postgres3="${sql_postgres3}${field_name},${NEWLINE}";
						if [[ $field_nullable == "Y" ]]
						then
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						else
							sql_postgres4="${sql_postgres4}r.${field_name},${NEWLINE}";
						fi
						sql_postgres5="${sql_postgres5}$(pop_postgres5 "${field_name}" "${conflict_field}" "${field_count}" "${counter}")"
					fi
					;;
				esac

				if [[ $counter -lt $field_count ]]
				then
					if [[ $field_type != "CLOB" ]] 
					then
						sql2_script="${sql2_script}||'|'||${NEWLINE}";
					fi
				else

					sql3_script="GREATEST(1,CEIL(NVL(DBMS_LOB.GETLENGTH(${l_clob_field}),0)/3900)) AS n_pieces";
					#sql2_script="${sql2_script}||'|#'${NEWLINE}";
				fi

				# Write out the postgres create schema file
				if [[ ${field_type} == "NUMBER" ]]
				then
					echo "${lower_field_name} NUMERIC,">>${postgres_schema_file};
				else
					if [[ ${field_type} == "CLOB" ]]
					then
						echo "${lower_field_name} TEXT,">>${postgres_schema_file};
						echo "marker TEXT,">>${postgres_schema_file};
					else
						echo "${lower_field_name} TEXT,">>${postgres_schema_file};
					fi
				fi
			
				field_nullable='';

	echo "sqlaa: $sql_script"
			done

			# Add the marker field
			echo "piece_no integer );${NEWLINE}">>${postgres_schema_file};
	echo "sql3: $sql_script"
			sql_script="${sql_script}${sql1_script}";
#			sql_script="${sql_script}${l_clob_field},${NEWLINE}";
			sql_script="${sql_script}${sql3_script}${NEWLINE}";
			sql_script="${sql_script}FROM ${tables_to_extract} AS OF SCN ${SCN}${NEWLINE}";

			# Shard the data
			sql_script_base="${sql_script}";
			adjusted_thread_count=$((thread_count -1));
echo "AAAAA ${thread_count}";
			for ((threads=0; threads<thread_count; threads++)); do
				l_have_where="NO";
echo "BBBBB";
echo "${threads}";
				# Populate the postgres commands file
				echo "\"c:\Program Files\PostgreSQL\16\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy ${postgres_schema}.${lower_table_name}_temp FROM '${lower_with_schema}_part_${threads}.csv' WITH (FORMAT text, DELIMITER '|', NULL '')\" \"${postgres_environment}\"">>${postgres_commands_file}_part_${threads}.bat;

				sql_spool="spool ${spool_location}/${tables_to_extract}_part_${threads}.csv;";
				sql_script=$sql_script_base;
echo "B1: ${l_have_where}"
				if [[ ${l_have_where} == "NO" ]]
				then
echo "B2: ${l_have_where}"
					sql_script="${sql_script}WHERE (ORA_HASH(TO_CHAR(${shard_field}), ${adjusted_thread_count-1}) = ${threads})${NEWLINE}";
				else
					sql_script="${sql_script}AND (ORA_HASH(TO_CHAR(${shard_field}), ${adjusted_thread_count-1}) = ${threads})${NEWLINE}";
echo "B3: ${l_have_where}"
				fi
				l_have_where="YES";
echo "B4: ${l_have_where}"

				echo 'ADDING $threads "@${tables_to_extract}_part_${threads}.sql${NEWLINE"';
				add_script_parallel $threads "@${tables_to_extract}_part_${threads}.sql${NEWLINE}";
echo "ADDING COMPLETE";
echo "CCCCC";
	
				# Do the incremental parameters
				if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "YES" ]
				then
echo "B5: ${l_have_where}"
					if [[ "${l_have_where}" == "NO" ]]
					then
echo "B6: ${l_have_where}"
						sql_script="${sql_script}WHERE${NEWLINE}";
					else
						sql_script="${sql_script}AND${NEWLINE}";
echo "B7: ${l_have_where}"
					fi
					l_have_where="YES";
echo "B8: ${l_have_where}"
					# Need to write the date filter
					# Have we done this table before
					record_count=`cat ${incremental_tracking_file}_${DATE_TIME}|grep ${tables_to_extract}|wc -l`;
					if [ $record_count -eq 1 ]
					then
						# extract the lower water mark
echo "before lwm"
echo "tables to extract ${tables_to_extract}";
echo "tracking file: ${incremental_tracking_file}";
						lwm_date="$(grep "^${tables_to_extract}#" "${incremental_tracking_file}_${DATE_TIME}" | awk -F'#' '{print $2}')"
						lwm_time="$(grep "^${tables_to_extract}#" "${incremental_tracking_file}_${DATE_TIME}" | awk -F'#' '{print $3}')"
echo "lwm_date: ${lwm_date}"
echo "lwm_time: ${lwm_time}"
						sql_script="${sql_script}FROM_TZ(CAST(${changed_date} AS TIMESTAMP), DBTIMEZONE) AT TIME ZONE 'UTC' > TO_TIMESTAMP_TZ('${lwm_date} ${lwm_time} UTC', 'YYYY-MM-DD HH24:MI:SS TZR')${NEWLINE}";
						sql_script="${sql_script}AND${NEWLINE}";
echo "${sql_script}";
					fi
					sql_script="${sql_script}FROM_TZ(CAST(${changed_date} AS TIMESTAMP), DBTIMEZONE) AT TIME ZONE 'UTC' <= TO_TIMESTAMP_TZ('${now_date} ${now_time} UTC', 'YYYY-MM-DD HH24:MI:SS TZR')${NEWLINE}";
echo "ZZ: ${sql_script}";
				fi
		
				sql_script="${sql_script}),${NEWLINE}";
				sql_script="${sql_script}maxn AS (SELECT MAX(n_pieces) AS max_pieces FROM base),${NEWLINE}";
				sql_script="${sql_script}seq AS (${NEWLINE}";
				sql_script="${sql_script}SELECT LEVEL AS piece_no FROM dual${NEWLINE}";
				sql_script="${sql_script}CONNECT BY LEVEL <= (SELECT NVL(max_pieces,1) FROM maxn)${NEWLINE}";
				sql_script="${sql_script})${NEWLINE}";
				sql_script="${sql_script}SELECT${NEWLINE}";
				sql_script="${sql_script}${sql2_script}||'|'||${NEWLINE}";
				sql_script="${sql_script}TO_CLOB(TO_CHAR(s.piece_no))${NEWLINE}";
#				sql_script="${sql_script}${l_clob_string}${NEWLINE}";
				sql_script="${sql_script}FROM base b${NEWLINE}";
				sql_script="${sql_script}JOIN seq s${NEWLINE}";
				sql_script="${sql_script}ON s.piece_no <= b.n_pieces${NEWLINE}";
				sql_script="${sql_script}ORDER BY b.${order_by_field}, s.piece_no;${NEWLINE}";
				sql_script="${sql_script}spool off;${NEWLINE}";
echo "sql_end: $sql_script";
echo "ABCD: ${sql_head}"
echo "DEFG"
				echo "${sql_head}">${tables_to_extract}_part_${threads}.sql;
				echo "${sql_spool}">>${tables_to_extract}_part_${threads}.sql;
				echo "${sql_script}">>${tables_to_extract}_part_${threads}.sql;
		
			done
echo "DDDDD";
			# Write the postgres to a file
			echo "${sql_postgres}">>$postgres_insert_file;
			echo "FROM ${postgres_schema}.${lower_table_name}_temp">>$postgres_insert_file;
			echo "),">>$postgres_insert_file;
			echo "${sql_postgres2}">>$postgres_insert_file;
			echo "FROM unescaped">>$postgres_insert_file;
			echo "GROUP BY ${conflict_field}">>$postgres_insert_file;
			echo ")">>$postgres_insert_file;
			echo "${sql_postgres3}">>$postgres_insert_file;
			echo ")">>$postgres_insert_file;
			echo "${sql_postgres4}">>$postgres_insert_file;
			echo "FROM reconstructed r">>$postgres_insert_file;
			if [[ ${conflict_constraint} == "NO" ]]
			then
				echo "ON CONFLICT (${conflict_field}) DO UPDATE SET">>$postgres_insert_file;
			else
				echo "ON CONFLICT ON CONSTRAINT ${conflict_constraint_name} DO UPDATE SET">>$postgres_insert_file;
			fi
			echo "${sql_postgres5}">>$postgres_insert_file;
			echo ";${NEWLINE}">>$postgres_insert_file;


			if [ $operation_mode == "INCREMENTAL" ] && [ $incremental_allowed == "YES" ]
			then
				# record the hwm in the tracking file
				echo "${tables_to_extract}#${now_date}#${now_time}#${SCN}">>${incremental_tracking_file};
			fi
			echo "sql4: $sql_script"

	fi
done

# Extract the drop and create sequence script
>extract_sequences.sql
echo "${sql_header_seq1}${NEWLINE}">>extract_sequences.sql;
echo "${sql_header_seq2}${NEWLINE}">>extract_sequences.sql;
echo "${sql_header_seq3}${NEWLINE}">>extract_sequences.sql;
for sequences_to_extract in $SEQUENCES_TO_EXTRACT
do
	sequence_owner=`echo ${sequences_to_extract}|awk -F"." '{print $1}'`
	sequence_name=`echo ${sequences_to_extract}|awk -F"." '{print $2}'`
	echo "select 'DROP SEQUENCE IF EXISTS appreg.${sequence_name};' from dual;">>extract_sequences.sql
	echo "select 'CREATE SEQUENCE appreg.${sequence_name} INCREMENT 1 MINVALUE 1 NO MAXVALUE START '||last_number||' CACHE '||cache_size||';' from dba_sequences where sequence_owner = '${sequence_owner}' and sequence_name = '${sequence_name}';">>extract_sequences.sql

done

# Write out the calling script
#echo "${calling_script}">extract_data.sql

# Extract the data to files
echo "EXTRACT"
max=${counts[$run]:-0};
idx=1
echo "max: ${max}";
echo "idx: ${idx}";
echo "script per parallel: ${script_per_parallel}";

for ((threads=0; threads<thread_count; threads++)); do
	while [ "$idx" -le "$max" ]; do
		flat=$(( run * BASE + threads ))
echo "flat: ${flat}";
		printf "	[%d] => %s\n" "$idx" "${script_per_parallel[$flat]}"
		idx=$((idx+1))
	done
done

echo "SSS";

# scan keys and populate max_idx + runs_array
for flat in ${!script_per_parallel[@]}; do
	# skip non numeric keys (defensive)
	if ! expr "$flat" + 0 >/dev/null 2>&1; then
		continue
	fi

	run=$(( flat / BASE ))
	idx=$(( flat % BASE ))
	
	# update max_idx for this run
	if [ -z "${max_idx[$run]:-}" ] || [ "${max_idx[$run]}" -lt "$idx" ]; then
		max_idx[$run]=$idx
	fi

	# add run to runs_array only if not present
	found=0
	for existing in "${runs_array[@]}"; do
		if [ "$existing" = "$run" ]; then
			found=1
			break
		fi
	done
	if [ $found -eq 0 ]; then
		runs_array=( "${runs_array[@]}" "$run" )
	fi
done

# if nothing found, exit gracefully
if [ "${#runs_array[@]}" -eq 0 ]; then
	echo "No entries in script_per_parallel - nothing to write"
	exit 0
fi

# Iterate runs safely and write files
for run in "${runs_array[@]}"; do
	# Debugging aid (uncomment if necessary}
	# echo "DEBUG: processing run='$run' max_idx='${max_idx[$run]}'"

	outfile="extract_data_${run}.sql"
	: > "$outfile" 		# truncate/create

	max=${max_idx[$run]:-0}
	# iterate from 1..max and write lines onlt from present entries
	i=1
	while [ "$i" -le "$max" ]; do
		flat=$(( run * BASE + i ))
		val="${script_per_parallel[$flat]:-}"
		if [ -n "$val" ]; then
			printf "%s\n" "$val" >> "$outfile"
		fi
		i=$(( i + 1 ))
	done

	echo "Wrote run $run -> $outfile (entries: $(wc -l < "$outfile"))"
done

		
# Do the deletes
echo "reverse"
echo "$TABLES_TO_EXTRACT" | tr ',' '\n' | tac | while read -r tables_to_extract; do
	# Work out our variables
	case $tables_to_extract in
		APPREGISTER.APPLICATION_CODES)
			echo "in APPLICATION_CODES"
			primary_key="AC_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='application_codes';
			lower_with_schema='appregister.application_codes';
			;;
		APPREGISTER.APPLICATION_LISTS)
			echo "in APPLICATION_LISTS"
			primary_key="AL_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='application_lists';
			lower_with_schema='appregister.application_lists';
			;;
		APPREGISTER.APPLICATION_LIST_ENTRIES)
			echo "in APPLICATION_LIST_ENTRIES"
			primary_key="ALE_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='application_list_entries';
			lower_with_schema='appregister.application_list_entries';
			;;
		APPREGISTER.APPLICATION_REGISTER)
			echo "in APPLICATION_REGISTER"
			primary_key="AR_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='application_register';
			lower_with_schema='appregister.application_register';
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_ID)
			echo "in APP_LIST_ENTRY_FEE_ID"
			primary_key="NVL(TO_CHAR(ALE_ALE_ID),'')||'|'||NVL(TO_CHAR(FEE_FEE_ID),'')||'|'||NVL(TO_CHAR(VERSION),'')||'|'||NVL(TO_CHAR(CHANGED_BY),'')||'|'||NVL(TO_CHAR(CHANGED_DATE,'YYYY-MM-DD HH24:MI:SS'),'')||'|'||REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(NVL(USER_NAME,''),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t') AS row_text";
			delete_allowed="YES";
			concatenated_key="YES";
			concatenated_string="row_text";
			lower_table_name='app_list_entry_fee_id';
			lower_with_schema='appregister.app_list_entry_fee_id';
			delete_statement="WITH parsed AS (
				SELECT 
				(a[1])::text AS ale_ale_id_txt,
				(a[2])::text AS fee_fee_id_txt,
				(a[3])::text AS version_txt,
				(a[4])::text AS changed_by_txt,
				(a[5])::timestamp AS changed_date,
				a[6] AS user_name_esc
				FROM (SELECT string_to_array(row_text,'|') AS a
					FROM ${postgres_schema}.${lower_table_name}_delete_temp) s
				),
				unescaped AS (
					SELECT 
					ale_ale_id_txt, fee_fee_id_txt, version_txt, changed_by_txt, changed_date,
					REPLACE(REPLACE(REPLACE(REPLACE(user_name_esc, '\p','|'),
						'\t', E'\t'),
						'\r', E'\r'),
						'\n', E'\n') AS user_name_mid
					FROM parsed
				),
				final_rows AS (
					SELECT 
						ale_ale_id_txt, fee_fee_id_txt, version_txt, changed_by_txt, changed_date,
					LEFT(REPLACE(user_name_mid, '\\','\'), 250)::varchar(250) as user_name
					FROM unescaped
				)
				DELETE FROM ${postgres_schema}.${lower_table_name} t
				USING final_rows d
				WHERE t.ale_ale_id::text = d.ale_ale_id_txt
				AND t.fee_fee_id::text = d.fee_fee_id_txt
				AND t.version::text = d.version_txt
				AND t.changed_by = d.changed_by_txt
				AND t.changed_date = d.changed_date
				AND t.user_name = d.user_name;";
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_STATUS)
			echo "in APP_LIST_ENTRY_FEE_STATUS"
			primary_key="ALEFS_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='app_list_entry_fee_status';
			lower_with_schema='appregister.app_list_entry_fee_status';
			;;
		APPREGISTER.APP_LIST_ENTRY_OFFICIAL)
			echo "in APP_LIST_ENTRY_OFFICIAL"
			primary_key="ALEO_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='app_list_entry_official';
			lower_with_schema='appregister.app_list_entry_official';
			;;
		APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS)
			echo "in APP_LIST_ENTRY_RESOLUTIONS"
			primary_key="ALER_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='app_list_entry_resolutions';
			lower_with_schema='appregister.app_list_entry_resolutions';
			;;
		APPREGISTER.CRIMINAL_JUSTICE_AREA)
			echo "in CRIMINAL_JUSTICE_AREA"
			primary_key="CJA_ID";
			delete_allowed="NO";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='criminal_justice_area';
			lower_with_schema='appregister.criminal_justice_area';
			;;
		APPREGISTER.DATA_AUDIT)
			echo "in DATA_AUDIT"
			primary_key="DATA_ID";
			delete_allowed="NO";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='data_audit';
			lower_with_schema='appregister.data_audit';
			;;
		APPREGISTER.FEE)
			echo "in FEE"
			primary_key="FEE_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='fee';
			lower_with_schema='appregister.fee';
			;;
		APPREGISTER.NAME_ADDRESS)
			echo "in NAME_ADDRESS"
			primary_key="NA_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='name_address';
			lower_with_schema='appregister.name_address';
			;;
		APPREGISTER.RESOLUTION_CODES)
			echo "in RESOLUTION_CODES"
			primary_key="RC_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='resolution_codes';
			lower_with_schema='appregister.resolution_codes';
			;;
		APPREGISTER.STANDARD_APPLICANTS)
			echo "in STANDARD_APPLICANTS"
			primary_key="SA_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='standard_applicants';
			lower_with_schema='appregister.standard_applicants';
			;;
		LIBRA.NATIONAL_COURT_HOUSES)
			echo "in NATIONAL_COURT_HOUSES"
			primary_key="NCH_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='national_court_houses';
			lower_with_schema='libra.national_court_houses';
			;;
		LIBRA.LINK_ADDRESSES)
			echo "in LINK_ADDRESSES"
			primary_key="LA_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='link_addresses';
			lower_with_schema='libra.link_addresses';
			;;
		LIBRA.ADDRESSES)
			echo "in ADDRESSES"
			primary_key="ADR_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='addresses';
			lower_with_schema='libra.addresses';
			;;
		LIBRA.LINK_COMMUNICATION_MEDIA)
			echo "in LINK_COMMUNICATION_MEDIA"
			primary_key="LCM_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='link_communication_media';
			lower_with_schema='libra.link_communication_media';
			;;
		LIBRA.COMMUNICATION_MEDIA)
			echo "in COMMUNICATION_MEDIA"
			primary_key="COMM_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='communication_media';
			lower_with_schema='libra.communication_media';
			;;
		LIBRA.PETTY_SESSIONAL_AREAS)
			echo "in PETTY_SESSIONAL_AREAS"
			primary_key="PSA_ID";
			delete_allowed="YES";
			concatenated_key="NO";
			concatenated_string="";
			lower_table_name='petty_sessional_areas';
			lower_with_schema='libra.petty_sessional_areas';
			;;
	esac

	if [[ "$delete_allowed" == "YES" ]]
	then
		if [[ "$FIRST_TIME" == "NO" ]]
		then
			# Find the old SCN number
			last_scn="$(grep "^${tables_to_extract}#" "${incremental_tracking_file}_${DATE_TIME}" | awk -F'#' '{print $4}')"
			echo "${tables_to_extract} ${last_scn}";
	
			# Start to generate the sql file
			echo "DEFINE LAST_SCN = '${last_scn}'">>deletes.sql
			echo "COLUMN THIS_SCN NEW_VALUE THIS_SCN">>deletes.sql
			echo "SELECT TO_CHAR(current_scn,'FM9999999999999999999999999999990') AS THIS_SCN FROM v\$database;">>deletes.sql
			echo "PROMPT Diffing deletes using (LAST_SCN=&LAST_SCN, THIS_SCN=&THIS_SCN)">>deletes.sql
			echo "SET PAGESIZE 0 HEADING OFF FEEDBACK OFF VERIFY OFF">>deletes.sql
			echo "SET LINESIZE 32767 WRAP OFF TRIMSPOOL OFF TAB OFF">>deletes.sql
			echo "SPOOL ${spool_location}/${tables_to_extract}.deletes.csv">>deletes.sql
			echo "WITH">>deletes.sql
			echo "old_keys AS (">>deletes.sql
			echo "SELECT ${primary_key} FROM ${tables_to_extract} AS OF SCN &LAST_SCN">>deletes.sql 
			echo "),">>deletes.sql
			echo "new_keys AS (">>deletes.sql
			echo "SELECT ${primary_key} FROM ${tables_to_extract} AS OF SCN &THIS_SCN">>deletes.sql 
			echo ")">>deletes.sql
			if [[ "$concatenated_key" == "YES" ]]
			then
				echo "SELECT ${concatenated_string}">>deletes.sql
				echo "FROM old_keys">>deletes.sql
				echo "MINUS">>deletes.sql
				echo "SELECT ${concatenated_string}">>deletes.sql
				echo "FROM new_keys">>deletes.sql

				# Create the schema for the deletes
				echo "DROP TABLE IF EXISTS ${postgres_schema}.${lower_table_name}_delete_temp;">>${postgres_delete_schema_file}
				echo "">>${postgres_delete_schema_file}
				echo "CREATE UNLOGGED TABLE IF NOT EXISTS ${postgres_schema}.${lower_table_name}_delete_temp (row_text TEXT);">>${postgres_delete_schema_file}
				echo "">>${postgres_delete_schema_file}

				# Write the commands to delete the data from 
				# the main postgres schema
				echo "${delete_statement}">>${postgres_delete_file};
				echo "">>${postgres_delete_file}
			else
				echo "SELECT TO_CHAR(${primary_key})">>deletes.sql
				echo "FROM old_keys">>deletes.sql
				echo "MINUS">>deletes.sql
				echo "SELECT TO_CHAR(${primary_key})">>deletes.sql
				echo "FROM new_keys">>deletes.sql
		
				# Create the schema for the deletes
				echo "DROP TABLE IF EXISTS ${postgres_schema}.${lower_table_name}_delete_temp;">>${postgres_delete_schema_file}
				echo "">>${postgres_delete_schema_file}
				echo "CREATE UNLOGGED TABLE IF NOT EXISTS ${postgres_schema}.${lower_table_name}_delete_temp (${primary_key} NUMERIC);">>${postgres_delete_schema_file}
				echo "">>${postgres_delete_schema_file}
	
				# Write the commands to delete the data from the main
				# postgres schema
				echo "delete from ${postgres_schema}.${lower_table_name} t">>${postgres_delete_file};
				echo "USING ${postgres_schema}.${lower_table_name}_delete_temp d">>${postgres_delete_file};
				echo "WHERE t.${primary_key} =  d.${primary_key};">>${postgres_delete_file};
				echo "">>${postgres_delete_file}
			fi
			echo "ORDER BY 1;">>deletes.sql
			echo "SPOOL OFF">>deletes.sql
			echo "">>deletes.sql
	
			# Populate the postgres commands file
			echo "\"c:\Program Files\PostgreSQL\16\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy ${postgres_schema}.${lower_table_name}_delete_temp FROM '${lower_with_schema}.deletes.csv' WITH (FORMAT text, DELIMITER '|', NULL '')\" \"${postgres_environment}\"">>$postgres_delete_commands_file;
	
		fi
	fi
done


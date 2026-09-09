#!/bin/bash

# Script:		extract_metadata.sh
#
# Purpose:		This script extracts all metadata from the Oracle 
#			database pertaining to the App Reg product.  This 
#			extract can then be used to compare with the Postgres
#			database, to proove that the data has been populated
#			correctly
#
# Usage:		sh ./extract_metadata.sh [scn]
#
# Note:			scn is optional - if not set no as of scn will be used
#
# Version History:
# Version	Date		Who		Purpose
# 1.0		29/08/2025	Matthew Harman	Initial Version
# 2.0		24/03/2026	Matthew Harman	Remove redundant tables
#						Add retention policy
# 3.0		11/08/2026	Matthew Harman	Add ability to pass scn to the
#						script, to extract data as of
#						that SCN.  ARCPOC-1685
# 4.0		14/08/2026	Matthew Harman	Add filtering of dataset as
#						per ARCPOC-1712
# 5.0		19/08/2026	Matthew Harman	Added better reporting as 
#						per ARCPOC-1684
#
# Configuration:	The following section should be modified to suit the
#			environment

# spool_location	Location to store extracted files
spool_location='/opt/moj/rman/appreg';

# postgres_metadata_commands_file	Location of the file created to have 
#					the commands to load the .csv's into
#					postgres
postgres_metadata_commands_file="${spool_location}/load_metadata.bat";

# postgres_environment			Postgres environment connection
#					string.
#					NOTE: Don't put passwords here
postgres_environment='postgresql://postgres:<pwd>@localhost:5432/appreg-db';

# retention_mode	Retention mode, YES to implement retention policy
#				i.e. we won't count data out of retention
#					NO to no retention policy in use
#				i.e. we will count all data
retention_mode='YES';

# retention_policy	Retention policy, date before which we will migrate
#				data.  Only applicable if retention_mode
#				above is set to YES
retention_policy='TRUNC(SYSDATE-1825)';

# TABLES_TO_EXTRACT	Stores a comma separated list of tables prefixed with
#			schema name that we need to migrate, with a third field
#			being the changed date field name, e.g.
#			<SCHEMA NAME>.<TABLE_NAME>.CHANGED_DATE
#			
# Removed APPREGISTER.DATA_AUDIT
TABLES_TO_EXTRACT='APPREGISTER.APPLICATION_CODES.CHANGED_DATE¬APPREGISTER.APPLICATION_LISTS.CHANGED_DATE¬APPREGISTER.APPLICATION_LIST_ENTRIES.CHANGED_DATE¬APPREGISTER.APPLICATION_REGISTER.CHANGED_DATE¬APPREGISTER.APP_LIST_ENTRY_FEE_ID.CHANGED_DATE¬APPREGISTER.APP_LIST_ENTRY_FEE_STATUS.ALEFS_CHANGED_DATE¬APPREGISTER.APP_LIST_ENTRY_OFFICIAL.CHANGED_DATE¬APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS.CHANGED_DATE¬APPREGISTER.CRIMINAL_JUSTICE_AREA.NO_FIELD¬APPREGISTER.FEE.FEE_CHANGED_DATE¬APPREGISTER.NAME_ADDRESS.CHANGED_DATE¬APPREGISTER.RESOLUTION_CODES.CHANGED_DATE¬APPREGISTER.STANDARD_APPLICANTS.CHANGED_DATE¬LIBRA.NATIONAL_COURT_HOUSES.CHANGED_DATE¬APPREGISTER.APPREG_USER_MAPPING.NO_FIELD';

# Table Structure to profile data
# One record for each table, stored via a case statement
# First field is field_name, second field type, third a replace if required
APPLICATION_CODES_STRUCTURE='AC_ID:NUMBER:NULL¬APPLICATION_CODE:VARCHAR:NULL¬APPLICATION_CODE_TITLE:VARCHAR:NULL¬APPLICATION_CODE_WORDING:CLOB:NULL¬APPLICATION_LEGISLATION:CLOB:NULL¬FEE_DUE:CHAR:NULL¬APPLICATION_CODE_RESPONDENT:CHAR:NULL¬AC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:NULL¬AC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:NULL¬APPLICATION_CODE_START_DATE:DATE:NULL¬APPLICATION_CODE_END_DATE:DATE:NULL¬BULK_RESPONDENT_ALLOWED:CHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL¬AC_FEE_REFERENCE:VARCHAR:NULL';
APPLICATION_LISTS_STRUCTURE="AL_ID:NUMBER:NULL¬APPLICATION_LIST_STATUS:VARCHAR:NULL¬APPLICATION_LIST_DATE:DATE:NULL¬APPLICATION_LIST_TIME:DATE:TO_CHAR(APPLICATION_LIST_TIME,'HH24:MI:SS')¬COURTHOUSE_CODE:VARCHAR:NULL¬OTHER_COURTHOUSE:VARCHAR:NULL¬LIST_DESCRIPTION:VARCHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL¬COURTHOUSE_NAME:VARCHAR:NULL¬DURATION_HOUR:NUMBER:NVL(DURATION_HOUR,0)¬DURATION_MINUTE:NUMBER:NVL(DURATION_MINUTE,0)¬CJA_CJA_ID:NUMBER:NULL";
APPLICATION_LIST_ENTRIES_STRUCTURE='ALE_ID:NUMBER:NULL¬AL_AL_ID:NUMBER:NULL¬SA_SA_ID:NUMBER:NULL¬AC_AC_ID:NUMBER:NULL¬A_NA_ID:NUMBER:NULL¬R_NA_ID:NUMBER:NULL¬NUMBER_OF_BULK_RESPONDENTS:NUMBER:NULL¬APPLICATION_LIST_ENTRY_WORDING:CLOB:NULL¬CASE_REFERENCE:VARCHAR:NULL¬ACCOUNT_NUMBER:VARCHAR:NULL¬ENTRY_RESCHEDULED:CHAR:NULL¬NOTES:VARCHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬BULK_UPLOAD:VARCHAR:NULL¬USER_NAME:VARCHAR:NULL¬SEQUENCE_NUMBER:NUMBER:NULL¬TCEP_STATUS:VARCHAR:NULL¬MESSAGE_UUID:VARCHAR:NULL¬RETRY_COUNT:VARCHAR:NULL¬LODGEMENT_DATE:DATE:NULL';
APPLICATION_REGISTER_STRUCTURE='AR_ID:NUMBER:NULL¬AL_AL_ID:NUMBER:NULL¬TEXT:CLOB:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL';
APP_LIST_ENTRY_FEE_ID_STRUCTURE='ALE_ALE_ID:NUMBER:NULL¬FEE_FEE_ID:NUMBER:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL';
APP_LIST_ENTRY_FEE_STATUS_STRUCTURE='ALEFS_ID:NUMBER:NULL¬ALEFS_ALE_ID:NUMBER:NULL¬ALEFS_PAYMENT_REFERENCE:VARCHAR:NULL¬ALEFS_FEE_STATUS:VARCHAR:NULL¬ALEFS_FEE_STATUS_DATE:DATE:NULL¬ALEFS_VERSION:NUMBER:NULL¬ALEFS_CHANGED_BY:NUMBER:NULL¬ALEFS_CHANGED_DATE:DATE:NULL¬ALEFS_USER_NAME:VARCHAR:NULL¬ALEFS_STATUS_CREATION_DATE:DATE:NULL';
APP_LIST_ENTRY_OFFICIAL_STRUCTURE='ALEO_ID:NUMBER:NULL¬ALE_ALE_ID:NUMBER:NULL¬TITLE:VARCHAR:NULL¬FORENAME:VARCHAR:NULL¬SURNAME:VARCHAR:NULL¬OFFICIAL_TYPE:VARCHAR:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL';
APP_LIST_ENTRY_RESOLUTIONS_STRUCTURE='ALER_ID:NUMBER:NULL¬RC_RC_ID:NUMBER:NULL¬ALE_ALE_ID:NUMBER:NULL¬AL_ENTRY_RESOLUTION_WORDING:CLOB:NULL¬AL_ENTRY_RESOLUTION_OFFICER:VARCHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL';
CRIMINAL_JUSTICE_AREA_STRUCTURE='CJA_ID:NUMBER:NULL¬CJA_CODE:VARCHAR:NULL¬CJA_DESCRIPTION:VARCHAR:NULL';
FEE_STRUCTURE='FEE_ID:NUMBER:NULL¬FEE_REFERENCE:VARCHAR:NULL¬FEE_DESCRIPTION:VARCHAR:NULL¬FEE_VALUE:NUMBER:NULL¬FEE_START_DATE:DATE:NULL¬FEE_END_DATE:DATE:NULL¬FEE_VERSION:NUMBER:NULL¬FEE_CHANGED_BY:NUMBER:NULL¬FEE_CHANGED_DATE:DATE:NULL¬FEE_USER_NAME:VARCHAR:NULL';
#NAME_ADDRESS_STRUCTURE='NA_ID:NUMBER:NULL¬CODE:VARCHAR:NULL¬NAME:VARCHAR:NULL¬TITLE:VARCHAR:NULL¬FORENAME_1:VARCHAR:NULL¬FORENAME_2:VARCHAR:NULL¬FORENAME_3:VARCHAR:NULL¬SURNAME:VARCHAR:NULL¬ADDRESS_L1:VARCHAR:NULL¬ADDRESS_L2:VARCHAR:NULL¬ADDRESS_L3:VARCHAR:NULL¬ADDRESS_L4:VARCHAR:NULL¬ADDRESS_L5:VARCHAR:NULL¬POSTCODE:VARCHAR:NULL¬EMAIL_ADDRESS:VARCHAR:NULL¬TELEPHONE_NUMBER:VARCHAR:NULL¬MOBILE_NUMBER:VARCHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL¬DATE_OF_BIRTH:DATE:NULL¬DMS_ID:VARCHAR:NULL';
NAME_ADDRESS_STRUCTURE='NA_ID:NUMBER:NULL¬CODE:VARCHAR:NULL¬NAME:VARCHAR:NULL¬TITLE:VARCHAR:NULL¬ADDRESS_L1:VARCHAR:NULL¬ADDRESS_L2:VARCHAR:NULL¬ADDRESS_L3:VARCHAR:NULL¬ADDRESS_L4:VARCHAR:NULL¬ADDRESS_L5:VARCHAR:NULL¬POSTCODE:VARCHAR:NULL¬EMAIL_ADDRESS:VARCHAR:NULL¬TELEPHONE_NUMBER:VARCHAR:NULL¬MOBILE_NUMBER:VARCHAR:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL¬DATE_OF_BIRTH:DATE:NULL¬DMS_ID:VARCHAR:NULL';
RESOLUTION_CODES_STRUCTURE='RC_ID:NUMBER:NULL¬RESOLUTION_CODE:VARCHAR:NULL¬RESOLUTION_CODE_TITLE:VARCHAR:NULL¬RESOLUTION_CODE_WORDING:CLOB:NULL¬RESOLUTION_LEGISLATION:CLOB:NULL¬RC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:NULL¬RC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:NULL¬RESOLUTION_CODE_START_DATE:DATE:TRUNC(RESOLUTION_CODE_START_DATE)¬RESOLUTION_CODE_END_DATE:DATE:TRUNC(RESOLUTION_CODE_END_DATE)¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL';
STANDARD_APPLICANTS_STRUCTURE='SA_ID:NUMBER:NULL¬STANDARD_APPLICANT_CODE:VARCHAR:NULL¬STANDARD_APPLICANT_START_DATE:DATE:NULL¬STANDARD_APPLICANT_END_DATE:DATE:NULL¬VERSION:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬USER_NAME:VARCHAR:NULL¬NAME:VARCHAR:NULL¬TITLE:VARCHAR:NULL¬FORENAME_1:VARCHAR:NULL¬FORENAME_2:VARCHAR:NULL¬FORENAME_3:VARCHAR:NULL¬SURNAME:VARCHAR:NULL¬ADDRESS_L1:VARCHAR:NULL¬ADDRESS_L2:VARCHAR:NULL¬ADDRESS_L3:VARCHAR:NULL¬ADDRESS_L4:VARCHAR:NULL¬ADDRESS_L5:VARCHAR:NULL¬POSTCODE:VARCHAR:NULL¬EMAIL_ADDRESS:VARCHAR:NULL¬TELEPHONE_NUMBER:VARCHAR:NULL¬MOBILE_NUMBER:VARCHAR:NULL';
NATIONAL_COURT_HOUSES_STRUCTURE='NCH_ID:NUMBER:NULL¬COURTHOUSE_NAME:VARCHAR:NULL¬VERSION_NUMBER:NUMBER:NULL¬CHANGED_BY:NUMBER:NULL¬CHANGED_DATE:DATE:NULL¬COURT_TYPE:VARCHAR:NULL¬START_DATE:DATE:NULL¬END_DATE:DATE:NULL¬LOC_LOC_ID:NUMBER:NULL¬PSA_PSA_ID:NUMBER:NULL¬COURT_LOCATION_CODE:VARCHAR:NULL¬SL_COURTHOUSE_NAME:VARCHAR:NULL¬NORG_ID:NUMBER:NULL';
APPREG_USER_MAPPING='LEGACY_CHANGED_BY:NUMBER:NULL¬MODERN_CHANGED_BY:VARCHAR:NULL';

# Further configuration that should not need changing
sql_header1="SET PAGESIZE 0 HEADING OFF FEEDBACK OFF VERIFY OFF";
sql_header2="SET LONG 1000000000 LONGCHUNKSIZE 10000";
sql_header3="SET LINESIZE 500 TRIMSPOOL OFF TAB OFF TERMOUT OFF ECHO OFF";

sql_header_seq1="SET PAGESIZE 0 HEADING OFF FEEDBACK OFF VERIFY OFF";
sql_header_seq2="SET LONG 1000000000 LONGCHUNKSIZE 10000";
sql_header_seq3="SET LINESIZE 500 TRIMSPOOL OFF TAB OFF TERMOUT OFF ECHO OFF";

# Main Code
calling_script="";
FIELD_SEPARATOR=$IFS
IFS='¬'
NEWLINE=$'\n'

# Populate the scn if it has been passed in as a parameter
SCN_VALUE="${1:-}"
if [[ -n "$SCN_VALUE" ]]; then
echo "SCN passed";
	HAVE_SCN="Y";
else
echo "SCN not passed";
	HAVE_SCN="N";
fi

# Loop through the TABLES
>${spool_location}/oracle_metadata.csv
>${spool_location}/oracle_rowcounts.csv
>${spool_location}/oracle_counts_by_date.csv
>${spool_location}/oracle_column_analysis.csv
>oracle_metadata.sql
>${postgres_metadata_commands_file}

for tables_to_extract in $TABLES_TO_EXTRACT
do
	schema_name=`echo ${tables_to_extract}|awk -F"." '{print $1}'`
	table_name=`echo ${tables_to_extract}|awk -F"." '{print $2}'`
	changed_date_field=`echo ${tables_to_extract}|awk -F"." '{print $3}'`

	# Setup data for later use in data analysis
	case $table_name in
        	APPLICATION_CODES)
	               	echo "in APPLICATION_CODES"
       	        	table_structure=$APPLICATION_CODES_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
        	APPLICATION_LISTS)
	               	echo "in APPLICATION_LISTS"
       	        	table_structure=$APPLICATION_LISTS_STRUCTURE;
			# No retention of APPLICATION_LISTS, the retention
			# is on tables that hang off it.
			retention_clause='';
			data_filter="";
       	         	;;
        	APPLICATION_LIST_ENTRIES)
	               	echo "in APPLICATION_LIST_ENTRIES"
       	        	table_structure=$APPLICATION_LIST_ENTRIES_STRUCTURE;
			retention_clause="AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy})))";
			data_filter="";
       	         	;;
        	APPLICATION_REGISTER)
	               	echo "in APPLICATION_REGISTER"
       	        	table_structure=$APPLICATION_REGISTER_STRUCTURE;
			retention_clause="AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy})))";
			data_filter="";
       	         	;;
        	APP_LIST_ENTRY_FEE_ID)
	               	echo "in APP_LIST_ENTRY_FEE_ID"
       	        	table_structure=$APP_LIST_ENTRY_FEE_ID_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
			data_filter="";
       	         	;;
        	APP_LIST_ENTRY_FEE_STATUS)
	               	echo "in APP_LIST_ENTRY_FEE_STATUS"
       	        	table_structure=$APP_LIST_ENTRY_FEE_STATUS_STRUCTURE;
			retention_clause="ALEFS_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
			data_filter="";
       	         	;;
        	APP_LIST_ENTRY_OFFICIAL)
	               	echo "in APP_LIST_ENTRY_OFFICIAL"
       	        	table_structure=$APP_LIST_ENTRY_OFFICIAL_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
			data_filter="";
       	         	;;
        	APP_LIST_ENTRY_RESOLUTIONS)
	               	echo "in APP_LIST_ENTRY_RESOLUTIONS"
       	        	table_structure=$APP_LIST_ENTRY_RESOLUTIONS_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
			data_filter="";
       	         	;;
        	CRIMINAL_JUSTICE_AREA)
	               	echo "in CRIMINAL_JUSTICE_AREA"
       	        	table_structure=$CRIMINAL_JUSTICE_AREA_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
        	FEE)
	               	echo "in FEE"
       	        	table_structure=$FEE_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
        	NAME_ADDRESS)
	               	echo "in NAME_ADDRESS"
       	        	table_structure=$NAME_ADDRESS_STRUCTURE;
			retention_clause="(NA_ID IN (SELECT A_NA_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (application_list_status = 'CLOSED' AND trunc(changed_date) > ${retention_policy})))) OR NA_ID IN (SELECT R_NA_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (application_list_status = 'CLOSED' AND trunc(changed_date) > ${retention_policy})))))";
			data_filter="";
       	         	;;
        	RESOLUTION_CODES)
	               	echo "in RESOLUTION_CODES"
       	        	table_structure=$RESOLUTION_CODES_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
        	STANDARD_APPLICANTS)
	               	echo "in STANDARD_APPLICANTS"
       	        	table_structure=$STANDARD_APPLICANTS_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
        	NATIONAL_COURT_HOUSES)
	               	echo "in NATIONAL_COURT_HOUSES"
       	        	table_structure=$NATIONAL_COURT_HOUSES_STRUCTURE;
			retention_clause='';
			data_filter="COURT_TYPE='CHOA'";
       	         	;;
        	APPREG_USER_MAPPING)
	               	echo "in APPREG_USER_MAPPING"
       	        	table_structure=$APPREG_USER_MAPPING_STRUCTURE;
			retention_clause='';
			data_filter="";
       	         	;;
	esac

	echo "starting extracting $tables_to_extract table structure at `date`"
	calling_script="${calling_script}@${tables_to_extract}.sql${NEWLINE}";

	# Need to loop through the fields
echo "aa: ${split_lob_into_chunks}";
	# Generate the sql script
	sql_script="${sql_header1}${NEWLINE}${sql_header2}";
	sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";

echo "sql: $sql_script"
	sql_script="${sql_script}spool ${spool_location}/oracle_metadata.csv append;${NEWLINE}";
echo "sql2: $sql_script"
	
	sql_script="${sql_script}SELECT${NEWLINE}";
	sql_script="${sql_script}''''||atc.owner||''','||${NEWLINE}";
	sql_script="${sql_script}''''||atc.table_name||''','||${NEWLINE}";
	sql_script="${sql_script}''''||atc.column_name||''','||${NEWLINE}";
	sql_script="${sql_script}''''||atc.data_type||''','||${NEWLINE}";
	sql_script="${sql_script}atc.char_length||','||${NEWLINE}";
	sql_script="${sql_script}''''||atc.nullable||''','||${NEWLINE}";
	sql_script="${sql_script}CASE WHEN atc.data_type IN ('VARCHAR2','NVARCHAR2','CHAR','NCHAR') THEN '''character varying('||CASE WHEN atc.char_used = 'C' THEN atc.char_length ELSE atc.data_length END || ')''' WHEN atc.data_type IN ('DATE','TIMESTAMP(6)') THEN '''timestamp without time zone'''${NEWLINE}";
	sql_script="${sql_script}WHEN atc.data_type = 'NUMBER' THEN CASE WHEN atc.data_precision < 5 THEN '''smallint''' ELSE '''numeric''' END${NEWLINE}";
	sql_script="${sql_script}ELSE NULL${NEWLINE}";
	sql_script="${sql_script}END AS row_data${NEWLINE}";
	sql_script="${sql_script}FROM all_tab_columns atc${NEWLINE}";
	sql_script="${sql_script}WHERE atc.owner = '${schema_name}'${NEWLINE}";
	sql_script="${sql_script}AND atc.table_name = '${table_name}'${NEWLINE}";
	sql_script="${sql_script}ORDER BY atc.table_name, atc.column_id;${NEWLINE}";
	sql_script="${sql_script}spool off;${NEWLINE}";
	echo "sqlaa: $sql_script"
	echo "${sql_script}">>oracle_metadata.sql;

	# Table row counts
	sql_script="${sql_header1}${NEWLINE}${sql_header2}";
	sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
	sql_script="${sql_script}spool ${spool_location}/oracle_rowcounts.csv append;${NEWLINE}";
	sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
	sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
	if [[ ${HAVE_SCN} == "Y" ]]; then
		sql_script="${sql_script}count(*) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
	else
		sql_script="${sql_script}count(*) FROM ${schema_name}.${table_name}${NEWLINE}";
	fi

	# Do we have a data filter?
	and_clause="WHERE";
	if [[ ! -z "${data_filter}" ]]
	then
		sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
		and_clause="AND";
	fi

	# Do we need to add in retention clause
	if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
	then
		sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
	else
		sql_script="${sql_script};${NEWLINE}";
	fi
	sql_script="${sql_script}spool off;${NEWLINE}";
	echo "sqlbb: $sql_script"
	echo "${sql_script}">>oracle_metadata.sql;

	# Do counts based on the changed date field
	if [ ${changed_date_field} != "NO_FIELD" ]; then
		sql_script="${sql_header1}${NEWLINE}${sql_header2}";
		sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
		sql_script="${sql_script}spool ${spool_location}/oracle_counts_by_date.csv append;${NEWLINE}";
		sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
		sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
		if [[ ${HAVE_SCN} == "Y" ]]; then
			sql_script="${sql_script}TO_CHAR(TRUNC(${changed_date_field}),'YYYY-MM-DD')||','||count(*) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
		else
			sql_script="${sql_script}TO_CHAR(TRUNC(${changed_date_field}),'YYYY-MM-DD')||','||count(*) FROM ${schema_name}.${table_name}${NEWLINE}";
		fi

		# Do we have a data filter?
		and_clause="WHERE";
		if [[ ! -z "${data_filter}" ]]
		then
			sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
			and_clause="AND";
		fi

		if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
		then
			sql_script="${sql_script} ${and_clause} ${retention_clause}${NEWLINE}";
		fi
		sql_script="${sql_script}group by TRUNC(${changed_date_field});${NEWLINE}";
		sql_script="${sql_script}spool off;${NEWLINE}";
		echo "sqlcc: $sql_script"
		echo "${sql_script}">>oracle_metadata.sql;
	fi

	# now profile the columns of data
echo "${table_structure}";
	for structure_info in $table_structure
	do
echo "a1";
echo "${structure_info}";
		field_name=`echo ${structure_info}|awk -F":" '{print $1}'`
		field_type=`echo ${structure_info}|awk -F":" '{print $2}'`
		replacement_clause=$(echo "${structure_info}" |awk '{
sub(/^[^:]*:[^:]*:/, "") 
print 
}');
echo "REPLACEMENT CLAUSE: ${replacement_clause}";

		case $field_type in 
			NUMBER) 
				echo "field ${field_name} is a number/date";
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min'||','||min(${replacement_clause}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'min'||','||min(${field_name}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min'||','||min(${replacement_clause}) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'min'||','||min(${field_name}) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]]
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="and";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max'||','||max(${replacement_clause}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'max'||','||max(${field_name}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max'||','||max(${replacement_clause}) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'max'||','||max(${field_name}) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]]
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# Null_count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||TO_CHAR(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||TO_CHAR(count(*) - count(${field_name})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||TO_CHAR(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||TO_CHAR(count(*) - count(${field_name})) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi
				
				and_clause="WHERE";
				# Do we have a data filter?
				if [[ ! -z "${data_filter}" ]]
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# distinct
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||count(distinct ${replacement_clause}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||count(distinct ${field_name}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||count(distinct ${replacement_clause}) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||count(distinct ${field_name}) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]]
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;
				;;
			DATE) 
				echo "field ${field_name} is a number/date";
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min'||','||min(${replacement_clause}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'min'||','||to_char(min(${field_name}),'YYYY-MM-DD HH24:MI:SS') FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min'||','||min(${replacement_clause}) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'min'||','||to_char(min(${field_name}),'YYYY-MM-DD HH24:MI:SS') FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max'||','||max(${replacement_clause}) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'max'||','||to_char(max(${field_name}), 'YYYY-MM-DD HH24:MI:SS') FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max'||','||max(${replacement_clause}) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'max'||','||to_char(max(${field_name}), 'YYYY-MM-DD HH24:MI:SS') FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# null count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${field_name})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${field_name})) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# distinct count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${replacement_clause})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${field_name})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${replacement_clause})) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${field_name})) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;
				;;
			CHAR|VARCHAR) 
				echo "field ${field_name} is a char/varchar";
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(length(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(length(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# null count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${field_name})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${replacement_clause})) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'null_count'||','||to_char(count(*) - count(${field_name})) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# distinct count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${replacement_clause})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${field_name})) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${replacement_clause})) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'distinct_count'||','||to_char(count(distinct ${field_name})) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# min length
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min_len'||','||to_char(nvl(min(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'min_len'||','||to_char(nvl(min(length(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min_len'||','||to_char(nvl(min(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'min_len'||','||to_char(nvl(min(length(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# max length
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max_len'||','||to_char(nvl(max(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'max_len'||','||to_char(nvl(max(length(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max_len'||','||to_char(nvl(max(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'max_len'||','||to_char(nvl(max(length(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# total length
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'sum_len'||','||to_char(nvl(sum(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'sum_len'||','||to_char(nvl(sum(length(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'sum_len'||','||to_char(nvl(sum(length(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'sum_len'||','||to_char(nvl(sum(length(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				and_clause="WHERE";
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;
				;;
			CLOB) 
				echo "field ${field_name} is a clob";
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
				else
					sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
				fi

				# Do we have a data filter?
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# null count
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					sql_script="${sql_script}'null_count'||','||to_char(sum(CASE WHEN ${field_name} IS NULL THEN 1 ELSE 0 END)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
				else
					sql_script="${sql_script}'null_count'||','||to_char(sum(CASE WHEN ${field_name} IS NULL THEN 1 ELSE 0 END)) FROM ${schema_name}.${table_name}${NEWLINE}";
				fi

				# Do we have a data filter?
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# minimum length
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min_len'||','||to_char(NVL(min(dbms_lob.getlength(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'min_len'||','||to_char(NVL(min(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'min_len'||','||to_char(NVL(min(dbms_lob.getlength(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'min_len'||','||to_char(NVL(min(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;

				# max length
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				if [[ ${HAVE_SCN} == "Y" ]]; then
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max_len'||','||to_char(NVL(max(dbms_lob.getlength(${replacement_clause})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					else
						sql_script="${sql_script}'max_len'||','||to_char(NVL(max(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name} AS OF SCN ${SCN_VALUE}${NEWLINE}";
					fi
				else
					if [[ "${replacement_clause}" != "NULL" ]]
					then
						sql_script="${sql_script}'max_len'||','||to_char(NVL(max(dbms_lob.getlength(${replacement_clause})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					else
						sql_script="${sql_script}'max_len'||','||to_char(NVL(max(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
					fi
				fi

				# Do we have a data filter?
				if [[ ! -z "${data_filter}" ]];
				then
					sql_script="${sql_script}${and_clause} ${data_filter}${NEWLINE}";
					and_clause="AND";
				fi

				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} ${and_clause} ${retention_clause};${NEWLINE}";
				else
					sql_script="${sql_script};${NEWLINE}";
				fi
				sql_script="${sql_script}spool off;${NEWLINE}";
				echo "sqlcc: $sql_script"
				echo "${sql_script}">>oracle_metadata.sql;
				;;

		esac

	done	
			
done

# Generate the file to load the csvs into postgres
echo "\"c:\Program Files\PostgreSQL\18\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy data_validation.oracle_column_metadata(owner, table_name, column_name, data_type, char_length, nullable, suggested_pg_type) FROM 'oracle_metadata.csv' CSV QUOTE ''''\" \"${postgres_environment}\"">>${postgres_metadata_commands_file};
echo "\"c:\Program Files\PostgreSQL\18\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy data_validation.oracle_rowcounts(owner, table_name, row_count) FROM 'oracle_rowcounts.csv' CSV QUOTE ''''\" \"${postgres_environment}\"">>${postgres_metadata_commands_file};
echo "\"c:\Program Files\PostgreSQL\18\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy data_validation.oracle_counts_by_date(owner, table_name, bucket_label, row_count) FROM 'oracle_counts_by_date.csv' CSV QUOTE ''''\" \"${postgres_environment}\"">>${postgres_metadata_commands_file};
echo "\"c:\Program Files\PostgreSQL\18\bin\psql.exe\" --set=ON_ERROR_STOP=1 -c \"\copy data_validation.oracle_column_analysis(owner, table_name, column_name, metric, metric_value) FROM 'oracle_column_analysis.csv' CSV QUOTE ''''\" \"${postgres_environment}\"">>${postgres_metadata_commands_file};


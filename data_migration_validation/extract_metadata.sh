#!/bin/bash

# Script:		extract_metadata.sh
#
# Purpose:		This script extracts all metadata from the Oracle 
#			database pertaining to the App Reg product.  This 
#			extract can then be used to compare with the Postgres
#			database, to proove that the data has been populated
#			correctly
#
# Usage:		sh ./extract_metadata.sh
#
# Version History:
# Version	Date		Who		Purpose
# 1.0		29/08/2025	Matthew Harman	Initial Version
# 2.0		24/03/2026	Matthew Harman	Remove redundant tables
#						Add retention policy
#
# Configuration:	The following section should be modified to suit the
#			environment

# spool_location	Location to store extracted files
spool_location='/opt/moj/rman/appreg';

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
TABLES_TO_EXTRACT='APPREGISTER.APPLICATION_CODES.CHANGED_DATE,APPREGISTER.APPLICATION_LISTS.CHANGED_DATE,APPREGISTER.APPLICATION_LIST_ENTRIES.CHANGED_DATE,APPREGISTER.APPLICATION_REGISTER.CHANGED_DATE,APPREGISTER.APP_LIST_ENTRY_FEE_ID.CHANGED_DATE,APPREGISTER.APP_LIST_ENTRY_FEE_STATUS.ALEFS_CHANGED_DATE,APPREGISTER.APP_LIST_ENTRY_OFFICIAL.CHANGED_DATE,APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS.CHANGED_DATE,APPREGISTER.CRIMINAL_JUSTICE_AREA.NO_FIELD,APPREGISTER.FEE.FEE_CHANGED_DATE,APPREGISTER.NAME_ADDRESS.CHANGED_DATE,APPREGISTER.RESOLUTION_CODES.CHANGED_DATE,APPREGISTER.STANDARD_APPLICANTS.CHANGED_DATE,LIBRA.NATIONAL_COURT_HOUSES.CHANGED_DATE';

# Table Structure to profile data
# One record for each table, stored via a case statement
APPLICATION_CODES_STRUCTURE='AC_ID:NUMBER,APPLICATION_CODE:VARCHAR,APPLICATION_CODE_TITLE:VARCHAR,APPLICATION_CODE_WORDING:CLOB,APPLICATION_LEGISLATION:CLOB,FEE_DUE:CHAR,APPLICATION_CODE_RESPONDENT:CHAR,AC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR,AC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR,APPLICATION_CODE_START_DATE:DATE,APPLICATION_CODE_END_DATE:DATE,BULK_RESPONDENT_ALLOWED:CHAR,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR,AC_FEE_REFERENCE:VARCHAR';
APPLICATION_LISTS_STRUCTURE='AL_ID:NUMBER,APPLICATION_LIST_STATUS:VARCHAR,APPLICATION_LIST_DATE:DATE,APPLICATION_LIST_TIME:DATE,COURTHOUSE_CODE:VARCHAR,OTHER_COURTHOUSE:VARCHAR,LIST_DESCRIPTION:VARCHAR,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR,COURTHOUSE_NAME:VARCHAR,DURATION_HOUR:NUMBER,DURATION_MINUTE:NUMBER,CJA_CJA_ID:NUMBER';
APPLICATION_LIST_ENTRIES_STRUCTURE='ALE_ID:NUMBER,AL_AL_ID:NUMBER,SA_SA_ID:NUMBER,AC_AC_ID:NUMBER,A_NA_ID:NUMBER,R_NA_ID:NUMBER,NUMBER_OF_BULK_RESPONDENTS:NUMBER,APPLICATION_LIST_ENTRY_WORDING:CLOB,CASE_REFERENCE:VARCHAR,ACCOUNT_NUMBER:VARCHAR,ENTRY_RESCHEDULED:CHAR,NOTES:VARCHAR,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,BULK_UPLOAD:VARCHAR,USER_NAME:VARCHAR,SEQUENCE_NUMBER:NUMBER,TCEP_STATUS:VARCHAR,MESSAGE_UUID:VARCHAR,RETRY_COUNT:VARCHAR,LODGEMENT_DATE:DATE';
APPLICATION_REGISTER_STRUCTURE='AR_ID:NUMBER,AL_AL_ID:NUMBER,TEXT:CLOB,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR';
APP_LIST_ENTRY_FEE_ID_STRUCTURE='ALE_ALE_ID:NUMBER,FEE_FEE_ID:NUMBER,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR';
APP_LIST_ENTRY_FEE_STATUS_STRUCTURE='ALEFS_ID:NUMBER,ALEFS_ALE_ID:NUMBER,ALEFS_PAYMENT_REFERENCE:VARCHAR,ALEFS_FEE_STATUS:VARCHAR,ALEFS_FEE_STATUS_DATE:DATE,ALEFS_VERSION:NUMBER,ALEFS_CHANGED_BY:NUMBER,ALEFS_CHANGED_DATE:DATE,ALEFS_USER_NAME:VARCHAR,ALEFS_STATUS_CREATION_DATE:DATE';
APP_LIST_ENTRY_OFFICIAL_STRUCTURE='ALEO_ID:NUMBER,ALE_ALE_ID:NUMBER,TITLE:VARCHAR,FORENAME:VARCHAR,SURNAME:VARCHAR,OFFICIAL_TYPE:VARCHAR,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR';
APP_LIST_ENTRY_RESOLUTIONS_STRUCTURE='ALER_ID:NUMBER,RC_RC_ID:NUMBER,ALE_ALE_ID:NUMBER,AL_ENTRY_RESOLUTION_WORDING:CLOB,AL_ENTRY_RESOLUTION_OFFICER:VARCHAR,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR';
CRIMINAL_JUSTICE_AREA_STRUCTURE='CJA_ID:NUMBER,CJA_CODE:VARCHAR,CJA_DESCRIPTION:VARCHAR';
FEE_STRUCTURE='FEE_ID:NUMBER,FEE_REFERENCE:VARCHAR,FEE_DESCRIPTION:VARCHAR,FEE_VALUE:NUMBER,FEE_START_DATE:DATE,FEE_END_DATE:DATE,FEE_VERSION:NUMBER,FEE_CHANGED_BY:NUMBER,FEE_CHANGED_DATE:DATE,FEE_USER_NAME:VARCHAR';
NAME_ADDRESS_STRUCTURE='NA_ID:NUMBER,CODE:VARCHAR,NAME:VARCHAR,TITLE:VARCHAR,FORENAME_1:VARCHAR,FORENAME_2:VARCHAR,FORENAME_3:VARCHAR,SURNAME:VARCHAR,ADDRESS_L1:VARCHAR,ADDRESS_L2:VARCHAR,ADDRESS_L3:VARCHAR,ADDRESS_L4:VARCHAR,ADDRESS_L5:VARCHAR,POSTCODE:VARCHAR,EMAIL_ADDRESS:VARCHAR,TELEPHONE_NUMBER:VARCHAR,MOBILE_NUMBER:VARCHAR,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR,DATE_OF_BIRTH:DATE,DMS_ID:VARCHAR';
RESOLUTION_CODES_STRUCTURE='RC_ID:NUMBER,RESOLUTION_CODE:VARCHAR,RESOLUTION_CODE_TITLE:VARCHAR,RESOLUTION_CODE_WORDING:CLOB,RESOLUTION_LEGISLATION:CLOB,RC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR,RC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR,RESOLUTION_CODE_START_DATE:DATE,RESOLUTION_CODE_END_DATE:DATE,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR';
STANDARD_APPLICANTS_STRUCTURE='SA_ID:NUMBER,STANDARD_APPLICANT_CODE:VARCHAR,STANDARD_APPLICANT_START_DATE:DATE,STANDARD_APPLICANT_END_DATE:DATE,VERSION:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,USER_NAME:VARCHAR,NAME:VARCHAR,TITLE:VARCHAR,FORENAME_1:VARCHAR,FORENAME_2:VARCHAR,FORENAME_3:VARCHAR,SURNAME:VARCHAR,ADDRESS_L1:VARCHAR,ADDRESS_L2:VARCHAR,ADDRESS_L3:VARCHAR,ADDRESS_L4:VARCHAR,ADDRESS_L5:VARCHAR,POSTCODE:VARCHAR,EMAIL_ADDRESS:VARCHAR,TELEPHONE_NUMBER:VARCHAR,MOBILE_NUMBER:VARCHAR';
NATIONAL_COURT_HOUSES_STRUCTURE='NCH_ID:NUMBER,COURTHOUSE_NAME:VARCHAR,VERSION_NUMBER:NUMBER,CHANGED_BY:NUMBER,CHANGED_DATE:DATE,COURT_TYPE:VARCHAR,START_DATE:DATE,END_DATE:DATE,LOC_LOC_ID:NUMBER,PSA_PSA_ID:NUMBER,COURT_LOCATION_CODE:VARCHAR,SL_COURTHOUSE_NAME:VARCHAR,NORG_ID:NUMBER';

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
IFS=','
NEWLINE=$'\n'

# Loop through the TABLES
>${spool_location}/oracle_metadata.csv
>${spool_location}/oracle_rowcounts.csv
>${spool_location}/oracle_counts_by_date.csv
>${spool_location}/oracle_column_analysis.csv
>oracle_metadata.sql

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
       	         	;;
        	APPLICATION_LISTS)
	               	echo "in APPLICATION_LISTS"
       	        	table_structure=$APPLICATION_LISTS_STRUCTURE;
			# No retention of APPLICATION_LISTS, the retention
			# is on tables that hang off it.
			retention_clause='';
       	         	;;
        	APPLICATION_LIST_ENTRIES)
	               	echo "in APPLICATION_LIST_ENTRIES"
       	        	table_structure=$APPLICATION_LIST_ENTRIES_STRUCTURE;
			retention_clause="AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy})))";
       	         	;;
        	APPLICATION_REGISTER)
	               	echo "in APPLICATION_REGISTER"
       	        	table_structure=$APPLICATION_REGISTER_STRUCTURE;
			retention_clause="AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy})))";
       	         	;;
        	APP_LIST_ENTRY_FEE_ID)
	               	echo "in APP_LIST_ENTRY_FEE_ID"
       	        	table_structure=$APP_LIST_ENTRY_FEE_ID_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
       	         	;;
        	APP_LIST_ENTRY_FEE_STATUS)
	               	echo "in APP_LIST_ENTRY_FEE_STATUS"
       	        	table_structure=$APP_LIST_ENTRY_FEE_STATUS_STRUCTURE;
			retention_clause="ALEFS_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
       	         	;;
        	APP_LIST_ENTRY_OFFICIAL)
	               	echo "in APP_LIST_ENTRY_OFFICIAL"
       	        	table_structure=$APP_LIST_ENTRY_OFFICIAL_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
       	         	;;
        	APP_LIST_ENTRY_RESOLUTIONS)
	               	echo "in APP_LIST_ENTRY_RESOLUTIONS"
       	        	table_structure=$APP_LIST_ENTRY_RESOLUTIONS_STRUCTURE;
			retention_clause="ALE_ALE_ID IN (SELECT ALE_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (APPLICATION_LIST_STATUS='CLOSED' AND trunc(changed_date) > ${retention_policy}))))";
       	         	;;
        	CRIMINAL_JUSTICE_AREA)
	               	echo "in CRIMINAL_JUSTICE_AREA"
       	        	table_structure=$CRIMINAL_JUSTICE_AREA_STRUCTURE;
			retention_clause='';
       	         	;;
        	FEE)
	               	echo "in FEE"
       	        	table_structure=$FEE_STRUCTURE;
			retention_clause='';
       	         	;;
        	NAME_ADDRESS)
	               	echo "in NAME_ADDRESS"
       	        	table_structure=$NAME_ADDRESS_STRUCTURE;
			retention_clause="(NA_ID IN (SELECT A_NA_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists WHERE (application_list_status = 'OPEN' OR (application_list_status = 'CLOSED' AND trunc(changed_date) > ${retention_policy})))) OR NA_ID IN (SELECT R_NA_ID FROM appregister.application_list_entries WHERE AL_AL_ID IN (SELECT AL_ID FROM appregister.application_lists where (application_list_status = 'OPEN' OR (application_list_status = 'CLOSED' AND trunc(changed_date) > ${retention_policy})))))";
       	         	;;
        	RESOLUTION_CODES)
	               	echo "in RESOLUTION_CODES"
       	        	table_structure=$RESOLUTION_CODES_STRUCTURE;
			retention_clause='';
       	         	;;
        	STANDARD_APPLICANTS)
	               	echo "in STANDARD_APPLICANTS"
       	        	table_structure=$STANDARD_APPLICANTS_STRUCTURE;
			retention_clause='';
       	         	;;
        	NATIONAL_COURT_HOUSES)
	               	echo "in NATIONAL_COURT_HOUSES"
       	        	table_structure=$NATIONAL_COURT_HOUSES_STRUCTURE;
			retention_clause='';
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
	sql_script="${sql_script}count(*) FROM ${schema_name}.${table_name}${NEWLINE}";
	# Do we need to add in retention clause
	if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
	then
		sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
		sql_script="${sql_script}TO_CHAR(TRUNC(${changed_date_field}),'YYYY-MM-DD')||','||count(*) FROM ${schema_name}.${table_name}${NEWLINE}";
		if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
		then
			sql_script="${sql_script} WHERE ${retention_clause}${NEWLINE}";
		fi
		sql_script="${sql_script}group by TRUNC(${changed_date_field});${NEWLINE}";
		sql_script="${sql_script}spool off;${NEWLINE}";
		echo "sqlcc: $sql_script"
		echo "${sql_script}">>oracle_metadata.sql;
	fi

	# now profile the columns of data
	for structure_info in $table_structure
	do
echo "a1";
		field_name=`echo ${structure_info}|awk -F":" '{print $1}'`
		field_type=`echo ${structure_info}|awk -F":" '{print $2}'`

		case $field_type in 
			NUMBER) 
				echo "field ${field_name} is a number/date";
				sql_script="${sql_header1}${NEWLINE}${sql_header2}";
				sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
				sql_script="${sql_script}spool ${spool_location}/oracle_column_analysis.csv append;${NEWLINE}";
				sql_script="${sql_script}SELECT '${schema_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${table_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'${field_name}'||','||${NEWLINE}";
				sql_script="${sql_script}'min'||','||min(${field_name}) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
				sql_script="${sql_script}'max'||','||max(${field_name}) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
				sql_script="${sql_script}'min'||','||min(to_char(${field_name},'YYYY-MM-DD HH24:MI:SS')) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
				sql_script="${sql_script}'max'||','||max(to_char(${field_name}, 'YYYY-MM-DD HH24:MI:SS')) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
				sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(length(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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
				sql_script="${sql_script}'avg_len'||','||to_char(NVL(avg(dbms_lob.getlength(${field_name})),0)) FROM ${schema_name}.${table_name}${NEWLINE}";
				if [[ ${retention_mode} == "YES" ]] && [[ ! -z "${retention_clause}" ]]
				then
					sql_script="${sql_script} WHERE ${retention_clause};${NEWLINE}";
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


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
#
# Configuration:	The following section should be modified to suit the
#			environment

# spool_location	Location to store extracted files
spool_location='/opt/moj/rman/appreg';

# TABLES_TO_EXTRACT	Stores a comma separated list of tables prefixed with
#			schema name that we need to migrate
# Removed APPREGISTER.DATA_AUDIT
TABLES_TO_EXTRACT='APPREGISTER.APPLICATION_CODES,APPREGISTER.APPLICATION_LISTS,APPREGISTER.APPLICATION_LIST_ENTRIES,APPREGISTER.APPLICATION_REGISTER,APPREGISTER.APP_LIST_ENTRY_FEE_ID,APPREGISTER.APP_LIST_ENTRY_FEE_STATUS,APPREGISTER.APP_LIST_ENTRY_OFFICIAL,APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS,APPREGISTER.CRIMINAL_JUSTICE_AREA,APPREGISTER.FEE,APPREGISTER.NAME_ADDRESS,APPREGISTER.RESOLUTION_CODES,APPREGISTER.STANDARD_APPLICANTS,LIBRA.NATIONAL_COURT_HOUSES,LIBRA.LINK_ADDRESSES,LIBRA.ADDRESSES,LIBRA.LINK_COMMUNICATION_MEDIA,LIBRA.COMMUNICATION_MEDIA,LIBRA.PETTY_SESSIONAL_AREAS';
SEQUENCES_TO_EXTRACT='APPREGISTER.AC_SEQ,LIBRA.ADR_SEQ,APPREGISTER.ALEFS_SEQ,APPREGISTER.ALEO_SEQ,APPREGISTER.ALER_SEQ,APPREGISTER.AL_SEQ,APPREGISTER.AR_SEQ,APPREGISTER.CJA_SEQ,LIBRA.COMM_SEQ,APPREGISTER.FEE_SEQ,LIBRA.LA_SEQ,LIBRA.LCM_SEQ,APPREGISTER.NA_SEQ,LIBRA.PSA_SEQ,APPREGISTER.RC_SEQ,APPREGISTER.SA_SEQ';


#TABLES_TO_EXTRACT='APPREGISTER.APPLICATION_CODES,APPREGISTER.APPLICATION_LISTS,APPREGISTER.APPLICATION_LIST_ENTRIES,APPREGISTER.APPLICATION_REGISTER,APPREGISTER.APP_LIST_ENTRY_FEE_ID,APPREGISTER.APP_LIST_ENTRY_FEE_STATUS,APPREGISTER.APP_LIST_ENTRY_OFFICIAL,APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS,APPREGISTER.CRIMINAL_JUSTICE_AREA,APPREGISTER.DATA_AUDIT,APPREGISTER.FEE,APPREGISTER.NAME_ADDRESS,APPREGISTER.RESOLUTION_CODES,APPREGISTER.STANDARD_APPLICANTS,LIBRA.NATIONAL_COURT_HOUSES,LIBRA.LINK_ADDRESSES,LIBRA.ADDRESSES,LIBRA.LINK_COMMUNICATION_MEDIA,LIBRA.COMMUNICATION_MEDIA,LIBRA.PETTY_SESSIONAL_AREAS';

# Table Fields		Each table has specific fields, detail them here
#		        NOTE: Add field type and whether NULL or NOT NULL
#			e.g. abc:VARCHAR:N is a field called abc which
#			is a VARCHAR and nulls not allowed (NOT NULL)
APPLICATION_CODES_FIELDS='AC_ID:NUMBER:N,APPLICATION_CODE:VARCHAR:N,APPLICATION_CODE_TITLE:VARCHAR:N,APPLICATION_CODE_WORDING:CLOB:N,APPLICATION_LEGISLATION:CLOB:Y,FEE_DUE:CHAR:N,APPLICATION_CODE_RESPONDENT:CHAR:N,AC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:Y,AC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:Y,APPLICATION_CODE_START_DATE:DATE:N,APPLICATION_CODE_END_DATE:DATE:Y,BULK_RESPONDENT_ALLOWED:CHAR:N,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y,AC_FEE_REFERENCE:VARCHAR:Y';

APPLICATION_LISTS_FIELDS='AL_ID:NUMBER:N,APPLICATION_LIST_STATUS:VARCHAR:Y,APPLICATION_LIST_DATE:DATE:N,APPLICATION_LIST_TIME:TIMESTAMP:N,COURTHOUSE_CODE:VARCHAR:Y,OTHER_COURTHOUSE:VARCHAR:Y,LIST_DESCRIPTION:VARCHAR:N,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y,COURTHOUSE_NAME:VARCHAR:Y,DURATION_HOUR:NUMBER:Y,DURATION_MINUTE:NUMBER:Y,CJA_CJA_ID:NUMBER:Y';

APPLICATION_LIST_ENTRIES_FIELDS='ALE_ID:NUMBER:N,AL_AL_ID:NUMBER:N,SA_SA_ID:NUMBER:Y,AC_AC_ID:NUMBER:N,A_NA_ID:NUMBER:Y,R_NA_ID:NUMBER:Y,NUMBER_OF_BULK_RESPONDENTS:NUMBER:Y,APPLICATION_LIST_ENTRY_WORDING:CLOB:N,CASE_REFERENCE:VARCHAR:Y,ACCOUNT_NUMBER:VARCHAR:Y,ENTRY_RESCHEDULED:CHAR:N,NOTES:VARCHAR:Y,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,BULK_UPLOAD:VARCHAR:Y,USER_NAME:VARCHAR:Y,SEQUENCE_NUMBER:NUMBER:N,TCEP_STATUS:VARCHAR:Y,MESSAGE_UUID:VARCHAR:Y,RETRY_COUNT:VARCHAR:Y,LODGEMENT_DATE:DATE:N';

APPLICATION_REGISTER_FIELDS='AR_ID:NUMBER:N,AL_AL_ID:NUMBER:N,TEXT:CLOB:Y,CHANGED_BY:NUMBER:N,CHANGED_DATE:TIMESTAMP:N,USER_NAME:VARCHAR:Y';

APP_LIST_ENTRY_FEE_ID_FIELDS='ALE_ALE_ID:NUMBER:N,FEE_FEE_ID:NUMBER:N,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:N';

APP_LIST_ENTRY_FEE_STATUS_FIELDS='ALEFS_ID:NUMBER:N,ALEFS_ALE_ID:NUMBER:N,ALEFS_PAYMENT_REFERENCE:VARCHAR:Y,ALEFS_FEE_STATUS:VARCHAR:N,ALEFS_FEE_STATUS_DATE:DATE:N,ALEFS_VERSION:NUMBER:N,ALEFS_CHANGED_BY:NUMBER:N,ALEFS_CHANGED_DATE:DATE:N,ALEFS_USER_NAME:VARCHAR:N,ALEFS_STATUS_CREATION_DATE:DATE:Y';

APP_LIST_ENTRY_OFFICIAL_FIELDS='ALEO_ID:NUMBER:N,ALE_ALE_ID:NUMBER:N,TITLE:VARCHAR:Y,FORENAME:VARCHAR:Y,SURNAME:VARCHAR:Y,OFFICIAL_TYPE:VARCHAR:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:N';

APP_LIST_ENTRY_RESOLUTIONS_FIELDS='ALER_ID:NUMBER:N,RC_RC_ID:NUMBER:N,ALE_ALE_ID:NUMBER:N,AL_ENTRY_RESOLUTION_WORDING:CLOB:N,AL_ENTRY_RESOLUTION_OFFICER:VARCHAR:N,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y';

CRIMINAL_JUSTICE_AREA_FIELDS='CJA_ID:NUMBER:N,CJA_CODE:VARCHAR:N,CJA_DESCRIPTION:VARCHAR:N';

DATA_AUDIT_FIELDS='DATA_ID:NUMBER:N,SCHEMA_NAME:VARCHAR:N,TABLE_NAME:VARCHAR:N,COLUMN_NAME:VARCHAR:N,OLD_VALUE:VARCHAR:Y,NEW_VALUE:VARCHAR:Y,USER_ID:VARCHAR:Y,LINK:VARCHAR:Y,CREATED_DATE:TIMESTAMP:N,OLD_CLOB_VALUE:CLOB:Y,NEW_CLOB_VALUE:CLOB:Y,RELATED_KEY:NUMBER:Y,UPDATE_TYPE:VARCHAR:N,DATA_TYPE:VARCHAR:Y,CASE_ID:NUMBER:Y,RELATED_ITEMS_IDENTIFIER:VARCHAR:Y,RELATED_ITEMS_IDENTIFIER_INDEX:VARCHAR:Y,EVENT_NAME:VARCHAR:Y,USER_NAME:VARCHAR:Y';

FEE_FIELDS='FEE_ID:NUMBER:N,FEE_REFERENCE:VARCHAR:N,FEE_DESCRIPTION:VARCHAR:N,FEE_VALUE:NUMBER:N,FEE_START_DATE:DATE:N,FEE_END_DATE:DATE:Y,FEE_VERSION:NUMBER:N,FEE_CHANGED_BY:NUMBER:N,FEE_CHANGED_DATE:DATE:N,FEE_USER_NAME:VARCHAR:N';

NAME_ADDRESS_FIELDS='NA_ID:NUMBER:N,CODE:VARCHAR:Y,NAME:VARCHAR:Y,TITLE:VARCHAR:Y,FORENAME_1:VARCHAR:Y,FORENAME_2:VARCHAR:Y,FORENAME_3:VARCHAR:Y,SURNAME:VARCHAR:Y,ADDRESS_L1:VARCHAR:N,ADDRESS_L2:VARCHAR:Y,ADDRESS_L3:VARCHAR:Y,ADDRESS_L4:VARCHAR:Y,ADDRESS_L5:VARCHAR:Y,POSTCODE:VARCHAR:Y,EMAIL_ADDRESS:VARCHAR:Y,TELEPHONE_NUMBER:VARCHAR:Y,MOBILE_NUMBER:VARCHAR:Y,VERSION:NUMBER:Y,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y,DATE_OF_BIRTH:DATE:Y,DMS_ID:VARCHAR:Y';

RESOLUTION_CODES_FIELDS='RC_ID:NUMBER:N,RESOLUTION_CODE:VARCHAR:N,RESOLUTION_CODE_TITLE:VARCHAR:N,RESOLUTION_CODE_WORDING:CLOB:N,RESOLUTION_LEGISLATION:CLOB:Y,RC_DESTINATION_EMAIL_ADDRESS_1:VARCHAR:Y,RC_DESTINATION_EMAIL_ADDRESS_2:VARCHAR:Y,RESOLUTION_CODE_START_DATE:DATE:N,RESOLUTION_CODE_END_DATE:DATE:Y,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y';

STANDARD_APPLICANTS_FIELDS='SA_ID:NUMBER:N,STANDARD_APPLICANT_CODE:VARCHAR:N,STANDARD_APPLICANT_START_DATE:DATE:N,STANDARD_APPLICANT_END_DATE:DATE:Y,VERSION:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,USER_NAME:VARCHAR:Y,NAME:VARCHAR:Y,TITLE:VARCHAR:Y,FORENAME_1:VARCHAR:Y,FORENAME_2:VARCHAR:Y,FORENAME_3:VARCHAR:Y,SURNAME:VARCHAR:Y,ADDRESS_L1:VARCHAR:N,ADDRESS_L2:VARCHAR:Y,ADDRESS_L3:VARCHAR:Y,ADDRESS_L4:VARCHAR:Y,ADDRESS_L5:VARCHAR:Y,POSTCODE:VARCHAR:Y,EMAIL_ADDRESS:VARCHAR:Y,TELEPHONE_NUMBER:VARCHAR:Y,MOBILE_NUMBER:VARCHAR:Y';

NATIONAL_COURT_HOUSES_FIELDS='NCH_ID:NUMBER:N,COURTHOUSE_NAME:VARCHAR:N,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,COURT_TYPE:VARCHAR:N,START_DATE:DATE:N,END_DATE:DATE:Y,LOC_LOC_ID:NUMBER:Y,PSA_PSA_ID:NUMBER:Y,COURT_LOCATION_CODE:VARCHAR:Y,SL_COURTHOUSE_NAME:VARCHAR:Y,NORG_ID:NUMBER:Y';

LINK_ADDRESSES_FIELDS='LA_ID:NUMBER:N,NO_FIXED_ABODE:VARCHAR:N,LA_TYPE:VARCHAR:N,START_DATE:DATE:N,END_DATE:DATE:Y,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,ADR_ADR_ID:NUMBER:N,BU_BU_ID:NUMBER:N,ER_ER_ID:NUMBER:Y,LOC_LOC_ID:NUMBER:Y,HEAD_OFFICE_INDICATOR:VARCHAR:Y';

ADDRESSES_FIELDS='ADR_ID:NUMBER:N,LINE1:VARCHAR:Y,LINE2:VARCHAR:Y,LINE3:VARCHAR:Y,LINE4:VARCHAR:Y,LINE5:VARCHAR:Y,POSTCODE:VARCHAR:Y,START_DATE:DATE:N,END_DATE:DATE:Y,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,MCC_MCC_ID:NUMBER:Y';

LINK_COMMUNICATION_MEDIA_FIELDS='LCM_ID:NUMBER:N,LCM_TYPE:VARCHAR:N,START_DATE:DATE:Y,END_DATE:DATE:Y,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,COMM_COMM_ID:NUMBER:N,LOC_LOC_ID:NUMBER:Y,ER_ER_ID:NUMBER:Y,BU_BU_ID:NUMBER:Y';

COMMUNICATION_MEDIA_FIELDS='COMM_ID:NUMBER:N,DETAIL:VARCHAR:N,START_DATE:DATE:N,END_DATE:DATE:Y,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N';

PETTY_SESSIONAL_AREAS_FIELDS='PSA_ID:NUMBER:N,PSA_NAME:VARCHAR:Y,SHORT_NAME:VARCHAR:Y,VERSION_NUMBER:NUMBER:N,CHANGED_BY:NUMBER:N,CHANGED_DATE:DATE:N,CMA_CMA_ID:NUMBER:Y,PSA_CODE:VARCHAR:N,START_DATE:DATE:Y,END_DATE:DATE:Y,JC_NAME:VARCHAR:Y,COURT_TYPE:VARCHAR:N,CRIME_CASES_LOC_ID:NUMBER:Y,FINE_ACCOUNTS_LOC_ID:NUMBER:Y,MAINTENANCE_ENFORCEMENT_LOC_ID:NUMBER:Y,FAMILY_CASES_LOC_ID:NUMBER:Y,COURT_LOCATION_CODE:VARCHAR:Y,CENTRAL_FINANCE_LOC_ID:NUMBER:Y,SL_PSA_NAME:VARCHAR:Y,NORG_ID:NUMBER:Y';

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

# Loop through the TABLES
for tables_to_extract in $TABLES_TO_EXTRACT
do
	echo "starting extracting $tables_to_extract at `date`"
	calling_script="${calling_script}@${tables_to_extract}.sql${NEWLINE}";
	case $tables_to_extract in
		APPREGISTER.APPLICATION_CODES)
			echo "in APPLICATION_CODES"
			table_fields=$APPLICATION_CODES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APPLICATION_LISTS)
			echo "in APPLICATION_LISTS"
			table_fields=$APPLICATION_LISTS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APPLICATION_LIST_ENTRIES)
			echo "in APPLICATION_LIST_ENTRIES"
			table_fields=$APPLICATION_LIST_ENTRIES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APPLICATION_REGISTER)
			echo "in APPLICATION_REGISTER"
			table_fields=$APPLICATION_REGISTER_FIELDS;
			split_lob_into_chunks="YES";
echo "split: ${split-lob_into_chunks}";
			order_by_field="AR_ID";
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_ID)
			echo "in APP_LIST_ENTRY_FEE_ID"
			table_fields=$APP_LIST_ENTRY_FEE_ID_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APP_LIST_ENTRY_FEE_STATUS)
			echo "in APP_LIST_ENTRY_FEE_STATUS"
			table_fields=$APP_LIST_ENTRY_FEE_STATUS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APP_LIST_ENTRY_OFFICIAL)
			echo "in APP_LIST_ENTRY_OFFICIAL"
			table_fields=$APP_LIST_ENTRY_OFFICIAL_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.APP_LIST_ENTRY_RESOLUTIONS)
			echo "in APP_LIST_ENTRY_RESOLUTIONS"
			table_fields=$APP_LIST_ENTRY_RESOLUTIONS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.CRIMINAL_JUSTICE_AREA)
			echo "in CRIMINAL_JUSTICE_AREA"
			table_fields=$CRIMINAL_JUSTICE_AREA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.DATA_AUDIT)
			echo "in DATA_AUDIT"
			table_fields=$DATA_AUDIT_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.FEE)
			echo "in FEE"
			table_fields=$FEE_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.NAME_ADDRESS)
			echo "in NAME_ADDRESS"
			table_fields=$NAME_ADDRESS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.RESOLUTION_CODES)
			echo "in RESOLUTION_CODES"
			table_fields=$RESOLUTION_CODES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		APPREGISTER.STANDARD_APPLICANTS)
			echo "in STANDARD_APPLICANTS"
			table_fields=$STANDARD_APPLICANTS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.NATIONAL_COURT_HOUSES)
			echo "in NATIONAL_COURT_HOUSES"
			table_fields=$NATIONAL_COURT_HOUSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.LINK_ADDRESSES)
			echo "in LINK_ADDRESSES"
			table_fields=$LINK_ADDRESSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.ADDRESSES)
			echo "in ADDRESSES"
			table_fields=$ADDRESSES_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.LINK_COMMUNICATION_MEDIA)
			echo "in LINK_COMMUNICATION_MEDIA"
			table_fields=$LINK_COMMUNICATION_MEDIA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.COMMUNICATION_MEDIA)
			echo "in COMMUNICATION_MEDIA"
			table_fields=$COMMUNICATION_MEDIA_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
		LIBRA.PETTY_SESSIONAL_AREAS)
			echo "in PETTY_SESSIONAL_AREAS"
			table_fields=$PETTY_SESSIONAL_AREAS_FIELDS;
			split_lob_into_chunks="NO";
			order_by_field="";
			;;
	esac

	# Need to loop through the fields
echo "aa: ${split_lob_into_chunks}";
	if [[ ${split_lob_into_chunks} == "NO" ]]
	then
		# not multi chunk 
		echo "table is not a multi chunk one";
		# Generate the sql script
		sql_script="${sql_header1}${NEWLINE}${sql_header2}";
		sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";

echo "sql: $sql_script"
		sql_script="${sql_script}spool ${spool_location}/${tables_to_extract}.csv;";
		sql_script="${sql_script}${NEWLINE}SELECT${NEWLINE}";
echo "sql2: $sql_script"

		# How many fields are in this table
		field_count=`echo $table_fields|wc -w`
		counter=0;

		# Loop through the fields
		for field_info in $table_fields
		do
			counter=`echo $counter+1|bc`;

			# We need to split the field_info into its 3 components
			field_name=`echo ${field_info}|awk -F":" '{print $1}'`
			field_type=`echo ${field_info}|awk -F":" '{print $2}'`
			field_nullable=`echo ${field_info}|awk -F":" '{print $3}'`

echo "AAA:field: $field_info"
echo "AAA:field_name: $field_name"
echo "AAA:field_type: $field_type"
echo "AAA:field_nullable: $field_nullable"

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
					;;
				CHAR)
					echo "field is a char";
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(NVL((${field_name},'')),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
					else
						sql_script="${sql_script}REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(${field_name}),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')"
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
					;;
				TIMESTAMP)
					echo "field is a timestamp";
					if [[ $field_nullable == "Y" ]]
					then
						sql_script="${sql_script}TO_CLOB(NVL(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS.FF6'),''))"
					else
						sql_script="${sql_script}TO_CLOB(TO_CHAR(${field_name},'YYYY-MM-DD HH24:MI:SS.FF6'))"
					fi
					;;
			esac
			field_nullable='';

			if [[ $counter -lt $field_count ]]
			then
				sql_script="${sql_script}||'|'||${NEWLINE}";
			else
				sql_script="${sql_script}||'|#'${NEWLINE}";
			fi
echo "sqlaa: $sql_script"
		done
echo "sql3: $sql_script"
		sql_script="${sql_script}FROM ${tables_to_extract}${NEWLINE}";
		if [[ ${order_by_field} -ne "" ]]
		then
			sql_script="${sql_script}ORDER BY ${order_by_field};";
		else
			sql_script="${sql_script};";
		fi
		sql_script="${sql_script}${NEWLINE}spool off;${NEWLINE}";

		# Write the script to a file
		echo "${sql_script}">${tables_to_extract}.sql;
	
echo "sql4: $sql_script"
	else
		# multi chunk 
		echo "table is a multi chunk one";
		# Generate the sql script
		sql_script="${sql_header1}${NEWLINE}${sql_header2}";
		sql_script="${sql_script}${NEWLINE}${sql_header3}${NEWLINE}";
		sql_script="${sql_script}spool ${spool_location}/${tables_to_extract}.csv;";

echo "sql: $sql_script";
		sql_script="${sql_script}${NEWLINE}WITH base AS (${NEWLINE}";
		sql_script="${sql_script}SELECT${NEWLINE}";
echo "sql2: $sql_script"

		# How many fields are in this table
		field_count=`echo $table_fields|wc -w`
		counter=0;

		sql1_script="";
		sql2_script="";

		# Loop through the fields
		for field_info in $table_fields
		do
			counter=`echo $counter+1|bc`;

			# We need to split the field_info into its 3 components
			field_name=`echo ${field_info}|awk -F":" '{print $1}'`
			field_type=`echo ${field_info}|awk -F":" '{print $2}'`
			field_nullable=`echo ${field_info}|awk -F":" '{print $3}'`

echo "AAA:field: $field_info"
echo "AAA:field_name: $field_name"
echo "AAA:field_type: $field_type"
echo "AAA:field_nullable: $field_nullable"

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
					;;
				CLOB)
echo "field_nullable: $field_nullable";
					echo "field is a clob ${field_name}";
					l_clob_string="REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TO_CLOB(DBMS_LOB.SUBSTR(b.${field_name},3900,1+(s.piece_no-1)*3900)),'\','\\\\'),'|','\p'),CHR(13),'\r'),CHR(10),'\n'),CHR(9),'\t')||'|#'"
echo "NOTNULL field"
					l_clob_field=${field_name};
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
						;;
				esac
				field_nullable='';

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
	echo "sqlaa: $sql_script"
			done
	echo "sql3: $sql_script"
			sql_script="${sql_script}${sql1_script}";
			sql_script="${sql_script}${l_clob_field},${NEWLINE}";
			sql_script="${sql_script}${sql3_script}${NEWLINE}";
			sql_script="${sql_script}FROM ${tables_to_extract}${NEWLINE}";
			sql_script="${sql_script}),${NEWLINE}";
			sql_script="${sql_script}maxn AS (SELECT MAX(n_pieces) AS max_pieces FROM base),${NEWLINE}";
			sql_script="${sql_script}seq AS (${NEWLINE}";
			sql_script="${sql_script}SELECT LEVEL AS piece_no FROM dual${NEWLINE}";
			sql_script="${sql_script}CONNECT BY LEVEL <= (SELECT max_pieces FROM maxn)${NEWLINE}";
			sql_script="${sql_script})${NEWLINE}";
			sql_script="${sql_script}SELECT${NEWLINE}";
			sql_script="${sql_script}${sql2_script}||'|'||${NEWLINE}";
			sql_script="${sql_script}TO_CLOB(TO_CHAR(s.piece_no))||'|'||${NEWLINE}";
			sql_script="${sql_script}${l_clob_string}${NEWLINE}";
			sql_script="${sql_script}FROM base b${NEWLINE}";
			sql_script="${sql_script}JOIN seq s${NEWLINE}";
			sql_script="${sql_script}ON s.piece_no <= b.n_pieces${NEWLINE}";
			sql_script="${sql_script}ORDER BY b.${order_by_field}, s.piece_no;${NEWLINE}";
			sql_script="${sql_script}spool off;${NEWLINE}";
echo "s	ql_end: $sql_script";
			echo "${sql_script}">${tables_to_extract}.sql;

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
echo "${calling_script}">extract_data.sql

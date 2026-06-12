#!/bin/bash

# This script retrieves data from the specified URL and saves the output to a file in a given folder.

# Check if the api key environment variable is set
if [ -z "$CSDS_KEY" ]; then
  echo "Error: CSDS_KEY environment variable is not set."
  exit 1
fi

LIMIT=50

BASE_URL="https://csds.dev.apps.hmcts.net/api/rest/query/CSDS/"
NAMED_QUERY_URL="https://csds.dev.apps.hmcts.net/api/rest/named-query/CSDS/"

TEMPLATES="./mappings/templates"

STANDARD_REFERENCE_TABLES=(
    ApplicationCode
    CivilFees
    CriminalJusticeAreas
    Court
    Venue
    ResolutionCodes
    Applicant
)

STANDARD_REFERENCE_URLS=(
    "ApplicationCode/GD?\$limit=${LIMIT}"
    "System/GD?\$f=Domain='CivilFees'&\$limit=${LIMIT}"
    "System/GD?\$f=Domain='CriminalJusticeAreas'&\$limit=${LIMIT}"
    "Court/GD?\$limit=${LIMIT}"
    "Venue/GD?\$limit=${LIMIT}"
    "ResolutionCodes/GD?\$limit=${LIMIT}"
    "Applicant/GD?\$limit=${LIMIT}"
)

# Create the output folder if it doesn't exist
OUTPUT_FOLDER="__files"
mkdir -p "$OUTPUT_FOLDER"

# Loop through the standard reference tables and retrieve data
for i in "${!STANDARD_REFERENCE_TABLES[@]}"; do
    TABLE_NAME="${STANDARD_REFERENCE_TABLES[$i]}"
    URL="${BASE_URL}${STANDARD_REFERENCE_URLS[$i]}"
    OUTPUT_FILE="${OUTPUT_FOLDER}/${TABLE_NAME}.json"
    COUNT_FILE="${OUTPUT_FOLDER}/${TABLE_NAME}_count.json"

    if [ $TABLE_NAME = "Address" ]; then
        echo "Will retrieve after getting applicant data"
        continue

    fi
    if [ $TABLE_NAME = "ContactInformation" ]; then
        echo "Will retrieve after getting applicant data..."
        continue
    fi

    echo "Retrieving data for ${TABLE_NAME} from ${URL}..."

    curl -H "Api-Key: ${CSDS_KEY}" "$URL" -o "$OUTPUT_FILE"

    if [ $? -eq 0 ]; then
        echo "Data for ${TABLE_NAME} saved to ${OUTPUT_FILE}"
    else
        echo "Error retrieving data for ${TABLE_NAME}"
    fi

    COUNT=$(jq '.records | length' "$OUTPUT_FILE")
    echo "Total records retrieved for ${TABLE_NAME}: ${COUNT}"

    echo "Saving wiremock count file for ${TABLE_NAME} to ${COUNT_FILE}"
    echo "{\"count\": ${COUNT}}" > "$COUNT_FILE"

    echo "Sleeping for a few seconds"
    sleep 5s
done

# Loop through the applicants json file and retrieve address and contact information for each applicant
APPLICANTS_FILE="${OUTPUT_FOLDER}/Applicant.json"
APPLICANT_FOLDER="${OUTPUT_FOLDER}/Applicants"
mkdir -p "$APPLICANT_FOLDER"

# Clear out folder before saving new data - we're assuming there is data in the folder
# So we want to clear it out so that the applicants json content matches the new applicant data we are fetching
rm -rf "${APPLICANT_FOLDER:?}"/*

if [ -f "$APPLICANTS_FILE" ]; then
    for id in $(jq -r '.records | .[] | .ApplicantID' "$APPLICANTS_FILE"); do
        echo "Retrieving address and contact information for applicant ID: ${id}..."
        ADDRESS_URL="${BASE_URL}Address/GD?\$f=FID_StandardApplicant=${id}"
        CONTACT_URL="${BASE_URL}ContactInformation/GD?\$f=FID_StandardApplicant=${id}"
        GET_STANDARD_APPLICANT_URL="${NAMED_QUERY_URL}GetStandardApplicant/GD?\$f=ApplicantId=${id}"

        OUTPUT_FILE="${APPLICANT_FOLDER}/Applicant_${id}_Address.json"

        echo "Retrieving data for Address from ${ADDRESS_URL}..."

        curl -H "Api-Key: ${CSDS_KEY}" "$ADDRESS_URL" -o "$OUTPUT_FILE"
        if [ $? -eq 0 ]; then
            echo "Data for Address saved to ${OUTPUT_FILE}"
        else
            echo "Error retrieving data for Address"
        fi

        OUTPUT_FILE="${APPLICANT_FOLDER}/Applicant_${id}_ContactDetails.json"

        curl -H "Api-Key: ${CSDS_KEY}" "$CONTACT_URL" -o "$OUTPUT_FILE"
        if [ $? -eq 0 ]; then
            echo "Data for ContactDetails saved to ${OUTPUT_FILE}"
        else
            echo "Error retrieving data for ContactDetails"
        fi

        curl -H "Api-Key: ${CSDS_KEY}" "$GET_STANDARD_APPLICANT_URL" -o "${APPLICANT_FOLDER}/Applicant_${id}_StandardApplicant.json"
        if [ $? -eq 0 ]; then
            echo "Data for StandardApplicant saved to ${APPLICANT_FOLDER}/Applicant_${id}_StandardApplicant.json"
        else
            echo "Error retrieving data for StandardApplicant"
        fi

        echo "Sleeping for a few seconds"
        sleep 3s
    done
fi


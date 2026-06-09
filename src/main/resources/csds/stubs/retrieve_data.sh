#!/bin/bash

# This script retrieves data from the specified URL and saves the output to a file in a given folder.

# Check if the api key environment variable is set
if [ -z "$CSDS_KEY" ]; then
  echo "Error: CSDS_KEY environment variable is not set."
  exit 1
fi

LIMIT=50

BASE_URL="https://csds.dev.apps.hmcts.net/api/rest/query/CSDS/"

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

    echo "Sleeping for a few seconds"
    sleep 5s
done

# Loop through the applicants json file and retrieve address and contact information for each applicant
APPLICANTS_FILE="${OUTPUT_FOLDER}/Applicant.json"
APPLICANT_FOLDER="${OUTPUT_FOLDER}/Applicants"
mkdir -p "$APPLICANT_FOLDER"

if [ -f "$APPLICANTS_FILE" ]; then
    for id in $(jq -r '.records | .[] | .ApplicantID' "$APPLICANTS_FILE"); do
        echo "Retrieving address and contact information for applicant ID: ${id}..."
        ADDRESS_URL="${BASE_URL}Address/GD?\$f=FID_StandardApplicant=${id}"
        CONTACT_URL="${BASE_URL}ContactInformation/GD?\$f=FID_StandardApplicant=${id}"

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

        echo "Sleeping for a few seconds"
        sleep 5s
    done
fi


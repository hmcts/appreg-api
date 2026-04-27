package uk.gov.hmcts.appregister.applicationentry.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * An enumeration to capture the errors for the application list entry.
 */
public enum AppListEntryError implements ErrorCodeEnum {
    RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The respondent type can only be an organisation or person",
                    "ALE-1")),

    APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The applicant type can only be an organisation, person, or standard applicant",
                    "ALE-2")),

    APPLICATION_CODE_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The supplied application code does not exist", "ALE-3")),

    MULTIPLE_APPLICATION_CODE_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "Multiple application codes exist", "ALE-4")),

    APPLICANT_CODE_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The supplied applicant code does not exist", "ALE-5")),

    FEE_REQUIRED(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "The code requires a fee", "ALE-6")),

    FEE_NOT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "The code does not require a fee", "ALE-7")),

    BULK_RESPONDENT_NOT_EXPECTED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Bulk respondent is not expected for the provided application code",
                    "ALE-8")),

    FEE_OFFSITE_NOT_SUITABLE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Offsite fee does not exist for code", "ALE-9")),

    STANDARD_APPLICANT_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Standard applicant does not exist for code", "ALE-10")),

    APPLICATION_LIST_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "The application list does not exist", "ALE-11")),

    APPLICATION_LIST_STATE_IS_INCORRECT(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "The application list state is not suitable to have an entry added for it",
                    "ALE-12")),
    RESPONDENT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Respondent is expected for the provided application code",
                    "ALE-13")),
    RESPONDENT_NOT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Respondent not expected for the provided application code",
                    "ALE-14")),

    ENTRY_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "Application entry does not exist", "ALE-15")),

    ENTRY_IS_NOT_WITHIN_LIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Application entry is not within application list",
                    "ALE-16")),

    MULTIPLE_STANDARD_APPLICANT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "Multiple Standard applicant exists for code", "ALE-17")),

    LIST_ENTRY_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "No application list entry was found that belongs to " + " the specified list",
                    "ALE-18")),

    PAYMENT_REFERENCE_NOT_ALLOWED_WHEN_PAYMENT_DUE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Payment reference must not be provided when payment status is DUE",
                    "ALE-19")),
    ACCOUNT_NUMBER_REQUIRED_FOR_APPLICATION_CODE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Account number is required for EF applications",
                    "ALE-20")),
    BULK_RESPONDENT_NUMBER_AND_RESPONDENT_MUTUALLY_EXCLUSIVE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Respondent and Bulk respondent number are mutually exclusive",
                    "ALE-21")),
    RESPONDENT_OR_NUMBER_OF_RESPONDENTS_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Either respondent details or number of respondents must be provided",
                    "ALE-22")),
    LODGEMENT_DATE_CANNOT_BE_IN_FUTURE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Lodgement date cannot be in the future", "ALE-23")),
    BULK_UPLOAD_FILE_MISSING(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Bulk upload file must be provided and not empty",
                    "ALE-24")),
    BULK_UPLOAD_INVALID_FILE_FORMAT(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Uploaded file must be a valid CSV file", "ALE-25")),
    BULK_UPLOAD_INVALID_HEADERS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "CSV header row is missing, invalid, or does not match the expected structure",
                    "ALE-26")),
    BULK_UPLOAD_INVALID_DELIMITER(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file delimiter. Expected pipe '|' separated values",
                    "ALE-27")),
    BULK_UPLOAD_ROW_VALIDATION_FAILED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "One or more rows failed validation during bulk upload",
                    "ALE-28")),
    BULK_UPLOAD_EMPTY_FILE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Uploaded file contains no data rows", "ALE-29")),
    BULK_UPLOAD_PROCESSING_FAILED(
            DefaultErrorDetail.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Bulk upload processing failed", "ALE-30"));
    private final DefaultErrorDetail defaultErrorCode;

    AppListEntryError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

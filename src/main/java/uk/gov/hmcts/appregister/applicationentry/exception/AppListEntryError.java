package uk.gov.hmcts.appregister.applicationentry.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * An enumeration to capture the errors for the application entry list.
 */
public enum AppListEntryError implements ErrorCodeEnum {
    RESPONDENT_CAN_ONLY_BE_ORGANISATION_OR_PERSON(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The respondent type can only be an organsisation or person",
                    "ALE-1")),

    APPLICANT_CAN_ONLY_BE_ORGANISATION_OR_PERSON(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The applicant type can only be an organsisation, person, or standard applicant",
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
                    HttpStatus.NOT_FOUND, "The code does not require a fee", "ALE-7")),

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
                    HttpStatus.NOT_FOUND, "The application list does not exist", "ALE-11")),

    APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE(
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
                    HttpStatus.NOT_FOUND, "Application entry does not exist", "ALE-15")),

    ENTRY_IS_NOT_WITHIN_LIST(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Application entry is not within application list",
                    "ALE-16")),

    MULTIPLE_STANDARD_APPLICANT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "Multiple Standard applicant exists for code", "ALE-17")),

    LIST_ENTRY_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "No application list entry was found that belongs to " + " the specified list",
                    "ALE-18"));

    private final DefaultErrorDetail defaultErrorCode;

    AppListEntryError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

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

    APPLICANT_CODE_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The supplied application code does not exist", "ALE-3")),

    FEE_REQUIRED(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "The code requires a fee", "ALE-5")),

    FEE_NOT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The code does not require a fee", "ALE-6")),

    BULK_RESPONDENT_NOT_EXPECTED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Bulk respondent is not expected for the provided application code",
                    "ALE-7")),

    FEE_OFFSITE_NOT_SUITABLE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Offsite fee does not exist for code", "ALE-8")),

    STANDARD_APPLICANT_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Standard applicant does not exist for code", "ALE-9")),

    APPLICATION_LIST_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The application list does not exist", "ALE-10")),

    APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The application list state is not suitable to have an entry added for it",
                    "ALE-11")),
    RESPONDENT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Respondent is expected for the provided application code",
                    "ALE-12")),
    NOT_RESPONDENT_REQUIRED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Respondent not expected for the provided application code",
                    "ALE-13"));

    private final DefaultErrorDetail defaultErrorCode;

    AppListEntryError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

package uk.gov.hmcts.appregister.applicationlist.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ApplicationListError implements ErrorCodeEnum {
    INVALID_LOCATION_COMBINATION(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Either 'courtLocation' must be provided, or both 'criminalJusticeArea'"
                            + " and 'otherLocationDescription' must be supplied.",
                    "AL-1")),
    CJA_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Criminal Justice Area not found", "AL-2")),
    DUPLICATE_CJA_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Criminal Justice Areas found when only one was expected",
                    "AL-3")),
    COURT_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Court Location not found", "AL-4")),
    DUPLICATE_COURT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Court Locations found when only one was expected",
                    "CL-5")),
    APPLICATION_LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application list not found", "CL-6")),

    DELETION_ID_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND,
                    "No application list found with the provided id for deletion",
                    "AL-2")),
    DELETION_ALREADY_IN_DELETABLE_STATE(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "The application list is not in a deletable state",
                    "AL-3"));
    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

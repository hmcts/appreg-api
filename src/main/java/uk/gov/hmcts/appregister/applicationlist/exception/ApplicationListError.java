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
                    "AL-5")),
    APPLICATION_LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application list not found", "AL-6")),

    DELETION_ID_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND,
                    "No application list found with the provided id for deletion",
                    "AL-7")),
    DELETION_ALREADY_IN_DELETABLE_STATE(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "The application list is not in a deletable state",
                    "AL-8")),
    LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application List not found", "AL-9")),
    ENTRY_NOT_PROVIDED(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "'entryIds' must be provided and non-empty", "AL-10")),
    INVALID_LIST_STATUS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Cannot move the applications because either the source or target list have a CLOSED 'status'",
                    "AL-11")),
    ENTRY_NOT_IN_SOURCE_LIST(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Application list entry not in source list", "AL-12")),
    SOURCE_LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application List not found", "AL-13")),
    TARGET_LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application List not found", "AL-14")),
    ENTRY_RESULT_LIST_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application List not found", "AL-15")),
    INVALID_ENTRY_RESULT_LIST_STATUS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete the entry result because the list has a CLOSED 'status'", "AL-16")),
    INVALID_NEW_LIST_STATUS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Cannot create the application because the " + "list has a CLOSED 'status'",
                    "AL-17")),
    INVALID_TIME(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Times in the format HH:MM:SS are not allowed in the application list; only the HH:MM format is"
                            + " supported.",
                    "AL-18"));
    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

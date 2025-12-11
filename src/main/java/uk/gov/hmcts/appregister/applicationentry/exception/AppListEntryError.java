package uk.gov.hmcts.appregister.applicationentry.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * An enumeration to capture the errors for the application entry list.
 */
public enum AppListEntryError implements ErrorCodeEnum {
    APPLICATION_LIST_DOES_NOT_EXIST(
        DefaultErrorDetail.create(
            HttpStatus.NOT_FOUND, "The application list does not exist", "ALE-11")),

    APPLICATION_LIST_STATE_IS_INCORRECT_FOR_CREATE(
        DefaultErrorDetail.create(
            HttpStatus.CONFLICT,
            "The application list state is not suitable to have an entry added for it",
            "ALE-12")),
    ENTRY_DOES_NOT_EXIST(
        DefaultErrorDetail.create(
            HttpStatus.NOT_FOUND, "Application entry does not exist", "ALE-15")),

    ENTRY_IS_NOT_WITHIN_LIST(
        DefaultErrorDetail.create(
            HttpStatus.BAD_REQUEST,
            "Application entry is not within application list",
            "ALE-16"));

    private final DefaultErrorDetail defaultErrorCode;

    AppListEntryError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

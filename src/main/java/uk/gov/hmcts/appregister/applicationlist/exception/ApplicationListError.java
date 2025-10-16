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

    /**
     * No Application List was found for the requested UUID.
     *
     * <p>HTTP 404 Not Found <br>
     */
    LIST_NOT_FOUND(
        DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Application List not found"));

    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

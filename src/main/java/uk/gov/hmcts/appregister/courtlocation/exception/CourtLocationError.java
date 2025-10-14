package uk.gov.hmcts.appregister.courtlocation.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * Domain-specific error codes for Court Location operations.
 *
 * <p>Each error maps to a standard {@link HttpStatus}, a human-readable message, and a stable
 * business error code (e.g. "CL-1").
 *
 * <p>Used in service and repository layers to signal known error conditions such as missing or
 * duplicate records.
 */
public enum CourtLocationError implements ErrorCodeEnum {

    /**
     * No active Court Location was found for the requested code.
     *
     * <p>HTTP 404 Not Found <br>
     * Business code: CL-1
     */
    COURT_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Court Location not found", "CL-1")),

    /**
     * More than one Court Location was found when only one was expected.
     *
     * <p>HTTP 409 Conflict <br>
     * Business code: CL-2
     */
    DUPLICATE_COURT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Court Locations found when only one was expected",
                    "CL-2"));

    /** Backing detail for the error code. */
    private final DefaultErrorDetail defaultErrorCode;

    CourtLocationError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

package uk.gov.hmcts.appregister.resolutioncode.exception;

import org.springframework.http.HttpStatus;

import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;

/**
 * Domain-specific error codes for Result Code operations.
 *
 * <p>Each error maps to a standard {@link HttpStatus}, a human-readable message, and a stable
 * business error code (e.g. "RC-1").
 *
 * <p>Used in service and repository layers to signal known error conditions such as missing or
 * duplicate records.
 */
public enum ResultCodeError implements ErrorCodeEnum {
    /**
     * No active Result Code was found for the requested code.
     *
     * <p>HTTP 404 Not Found <br>
     * Business code: RC-1
     */
    RESULT_CODE_NOT_FOUND(
        DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Result Code not found", "RC-1")),

    /**
     * More than one Result Code was found when only one was expected.
     *
     * <p>HTTP 409 Conflict <br>
     * Business code: RC-2
     */
    DUPLICATE_RESULT_CODE_FOUND(
        DefaultErrorDetail.create(
            HttpStatus.CONFLICT,
            "Multiple Result Codes found when only one was expected",
            "RC-2"));

    /** Backing detail for the error code. */
    private final DefaultErrorDetail defaultErrorCode;

    ResultCodeError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

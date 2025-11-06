package uk.gov.hmcts.appregister.standardapplicant.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * Domain-specific error codes for Standard Applicant operations.
 *
 * <p>Each error maps to a standard {@link org.springframework.http.HttpStatus}, a human-readable message, and a stable
 * business error code (e.g. "SA-1").
 *
 * <p>Used in service and repository layers to signal known error conditions such as missing or
 * duplicate records.
 */
public enum StandardApplicantCodeError implements ErrorCodeEnum {
    /**
     * No active Standard Applicant was found for the requested code.
     *
     * <p>HTTP 404 Not Found <br>
     * Business code: RC-1
     */
    STANDARD_APPLICANT_NOT_FOUND(
            DefaultErrorDetail.create(HttpStatus.NOT_FOUND, "Standard Applicant not found", "SA-1")),
    /**
     * More than one Standard Applicant was found when only one was expected.
     *
     * <p>HTTP 409 Conflict <br>
     * Business code: RC-2
     */
    DUPLICATE_RESULT_CODE_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Standard Applicant Codes found when only one was expected",
                    "SA-2"));

    /** Backing detail for the error code. */
    private final DefaultErrorDetail defaultErrorCode;

    StandardApplicantCodeError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

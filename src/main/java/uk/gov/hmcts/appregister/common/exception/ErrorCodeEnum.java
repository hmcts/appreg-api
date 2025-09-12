package uk.gov.hmcts.appregister.common.exception;

/** An interface for enums that represent error codes. */
public interface ErrorCodeEnum {
    /**
     * Each enum entry has an error code.
     *
     * @return The error code
     */
    ErrorCode getCode();
}

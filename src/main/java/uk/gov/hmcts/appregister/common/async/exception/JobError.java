package uk.gov.hmcts.appregister.common.async.exception;

import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

import org.springframework.http.HttpStatus;

/**
 * An enumeration to capture the errors for the application list entry.
 */
public enum JobError implements ErrorCodeEnum {
    JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The requested job does not exist or it is not for the user",
                    "ALE-1"));

    private final DefaultErrorDetail defaultErrorCode;

    JobError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

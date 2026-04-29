package uk.gov.hmcts.appregister.common.async.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * An enumeration to capture the errors for the asynchronous jobs that can be thrown back to the
 * client synchronously.
 */
public enum JobError implements ErrorCodeEnum {
    JOB_DOES_NOT_EXIST_OR_NOT_FOR_USER(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND,
                    "The requested job does not exist or it is not for the user",
                    "JOB-1")),
    JOB_TYPE_IS_ALREADY_RUNNING(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "The requested job type is already running", "JOB-2")),
    JOB_DOES_NOT_HAVE_DATA_TO_GET_A_DOWNLOAD_STREAM(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "The requested job does not have data to get a download stream",
                    "JOB-3")),
    JOB_STATE_IS_NOT_SUITABLE_FOR_DOWNLOAD(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "The job status is not correct", "JOB-4"));

    private final DefaultErrorDetail defaultErrorCode;

    JobError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

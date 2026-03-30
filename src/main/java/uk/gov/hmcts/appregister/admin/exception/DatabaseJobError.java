package uk.gov.hmcts.appregister.admin.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum DatabaseJobError implements ErrorCodeEnum {
    JOB_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Database Job not found", "DATABASEJOB-1"));

    private final DefaultErrorDetail defaultErrorCode;

    DatabaseJobError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return this.defaultErrorCode;
    }
}

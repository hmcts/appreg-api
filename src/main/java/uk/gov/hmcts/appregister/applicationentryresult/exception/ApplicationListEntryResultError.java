package uk.gov.hmcts.appregister.applicationentryresult.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ApplicationListEntryResultError implements ErrorCodeEnum {
    LIST_ENTRY_RESULT_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "No application list entry result was found that belongs to the specified entry",
                    "ALER-1"));
    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListEntryResultError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

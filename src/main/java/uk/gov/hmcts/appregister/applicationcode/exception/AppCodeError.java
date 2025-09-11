package uk.gov.hmcts.appregister.applicationcode.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;

public enum AppCodeError implements ErrorCodeEnum {
    // TODO: We need to apply an application code here
    CODE_NOT_FOUND(
            DefaultErrorCode.create(HttpStatus.NOT_FOUND, "Application Code not found", "appcode")),
    SORT_NOT_SUITABLE(DefaultErrorCode.create(HttpStatus.BAD_REQUEST, "Sort", "appcode"));

    private final DefaultErrorCode defaultErrorCode;

    AppCodeError(DefaultErrorCode defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorCode getCode() {
        return defaultErrorCode;
    }
}

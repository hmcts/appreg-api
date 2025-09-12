package uk.gov.hmcts.appregister.applicationcode.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;

/** The app code errors that will be represented as problem details when exceptions are thrown. */
public enum AppCodeError implements ErrorCodeEnum {
    CODE_NOT_FOUND(
            DefaultErrorCode.create(
                    HttpStatus.NOT_FOUND, "Application Code not found", "APPCODE-1"));

    private final DefaultErrorCode defaultErrorCode;

    AppCodeError(DefaultErrorCode defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorCode getCode() {
        return defaultErrorCode;
    }
}

package uk.gov.hmcts.appregister.applicationcode.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/** The app code errors that will be represented as problem details when exceptions are thrown. */
public enum AppCodeError implements ErrorCodeEnum {
    CODE_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Application code not found", "APPCODE-1")),
    SORT_NOT_SUITABLE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Sort key not suitable", "APPCODE-2"));

    private final DefaultErrorDetail defaultErrorCode;

    AppCodeError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

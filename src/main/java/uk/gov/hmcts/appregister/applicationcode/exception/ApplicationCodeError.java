package uk.gov.hmcts.appregister.applicationcode.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * The application code errors that will be represented as problem details when exceptions are
 * thrown.
 */
public enum ApplicationCodeError implements ErrorCodeEnum {
    CODE_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Application code not found", "APPCODE-1")),

    /**
     * More than one Application Code was found when only one was expected.
     *
     * <p>HTTP 409 Conflict <br>
     * Business code: APPCODE-2
     */
    DUPLICATE_COURT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Application Codes found when only one was expected",
                    "APPCODE-2"));

    private final DefaultErrorDetail defaultErrorCode;

    ApplicationCodeError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

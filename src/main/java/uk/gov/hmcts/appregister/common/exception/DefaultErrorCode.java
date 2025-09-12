package uk.gov.hmcts.appregister.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** Describes a default error code containing http status, message and application code. */
@RequiredArgsConstructor
@Getter
public class DefaultErrorCode implements ErrorCode {
    private final HttpStatus httpCode;
    private final String message;
    private final String appCode;

    public static DefaultErrorCode createNotFoundRequest(ErrorCodeEnum detail) {
        return new DefaultErrorCode(
                HttpStatus.NOT_FOUND, detail.getCode().getMessage(), detail.getCode().getAppCode());
    }

    public static DefaultErrorCode create(HttpStatus status, String message, String appCode) {
        return new DefaultErrorCode(status, message, appCode);
    }

    public static ErrorCodeEnum getEnumEntry(ErrorCode code) {
        return new ErrorCodeEnum() {
            @Override
            public ErrorCode getCode() {
                return code;
            }
        };
    }

    @Override
    public HttpStatus getHttpCode() {
        return httpCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getAppCode() {
        return appCode;
    }
}

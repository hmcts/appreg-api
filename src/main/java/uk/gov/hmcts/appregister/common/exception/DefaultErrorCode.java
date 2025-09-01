package uk.gov.hmcts.appregister.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public class DefaultErrorCode implements ErrorCode{
    private final HttpStatus httpCode;
    private final String message;
    private final String appCode;

    public static DefaultErrorCode createBadRequest(ErrorCodeEnum detail) {
        return new DefaultErrorCode(HttpStatus.BAD_REQUEST, detail.getCode().getMessage(), detail.getCode().getAppCode());
    }

    public static DefaultErrorCode create(HttpStatus status, String message) {
        return new DefaultErrorCode(HttpStatus.BAD_REQUEST, message, null);
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

package uk.gov.hmcts.appregister.common.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Describes a default error code containing http status, message and application code.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class DefaultErrorDetail implements ErrorDetail {
    private final HttpStatus httpCode;
    private final String message;
    private final String appCode;

    public static DefaultErrorDetail create(HttpStatus status, String message, String appCode) {
        return new DefaultErrorDetail(status, message, appCode);
    }

    public static DefaultErrorDetail create(HttpStatus status, String message) {
        return new DefaultErrorDetail(status, message, null);
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

package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;

public enum CommonAppError implements ErrorCodeEnum {
    SORT_NOT_SUITABLE(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Sort key not suitable", "COMMON-1"));

    private final DefaultErrorDetail defaultErrorCode;

    CommonAppError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

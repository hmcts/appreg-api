package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;

public enum CommonAppError implements ErrorCodeEnum {
    SORT_NOT_SUITABLE(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Sort key not suitable", "COMMON-1")),
    MATCH_ETAG_FAILURE(
            DefaultErrorDetail.create(HttpStatus.PRECONDITION_FAILED, "Etag failure", "COMMON-2")),
    SORT_DIRECTION_NOT_SUITABLE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Sort direction not suitable", "COMMON-3"));

    private final DefaultErrorDetail defaultErrorCode;

    CommonAppError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

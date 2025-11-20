package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;

public enum CommonAppError implements ErrorCodeEnum {
    SORT_NOT_SUITABLE(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Sort key not suitable", "COMMON-1")),
    INTERNAL_SERVER_ERROR(
            DefaultErrorDetail.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "General unexpected failure", "COMMON-2")),
    MATCH_ETAG_FAILURE(
            DefaultErrorDetail.create(HttpStatus.PRECONDITION_FAILED, "Etag failure", "COMMON-3")),
    SORT_DIRECTION_NOT_SUITABLE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Sort direction not suitable", "COMMON-4")),
    CONSTRAINT_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Constraint Error", "COMMON-5")),
    TYPE_MISMATCH_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Type Mismatch Error", "COMMON-6")),
    METHOD_ARGUMENT_INVALID_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Method Error", "COMMON-7")),
    METHOD_VALIDATION_INVALID_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Method Invalid Type", "COMMON-8"));

    private final DefaultErrorDetail defaultErrorCode;

    CommonAppError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

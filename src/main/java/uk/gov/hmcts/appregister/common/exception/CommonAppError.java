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
    WORDING_DATA_TYPE_FAILURE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Wording data type failure", "COMMON-5")),
    WORDING_LENGTH_FAILURE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Wording length exceeded failure", "COMMON-6")),
    WORDING_TEMPLATE_FORMAT_FAILURE(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Template format failure", "COMMON-7")),
    WORDING_SUBSTITUTE_SIZE_MISMATCH(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Values to be substituted does not match substituting values",
                    "COMMON-8")),
    WORDING_SUBSTITUTE_KEY_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Substitution key not found in the template",
                    "COMMON-15")),
    CONSTRAINT_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Constraint Error", "COMMON-9")),
    TYPE_MISMATCH_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Type Mismatch Error", "COMMON-10")),
    METHOD_ARGUMENT_INVALID_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Method Error", "COMMON-11")),
    METHOD_VALIDATION_INVALID_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Method Invalid Type", "COMMON-12")),
    NOT_READABLE_ERROR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Not Readable Error", "COMMON-13")),
    PARAMETER_REQUIRED(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Parameter required", "COMMON-14")),
    REGEX_VALIDATION_MISMATCH_ERROR(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Regex Validation Mismatch", "COMMON-16"));

    private final DefaultErrorDetail defaultErrorCode;

    CommonAppError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

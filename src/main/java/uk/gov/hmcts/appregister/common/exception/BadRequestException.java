package uk.gov.hmcts.appregister.common.exception;

public class BadRequestException extends AppRegistryException {
    public BadRequestException(ErrorCodeEnum errorCodeEnum, String message, Throwable cause) {
        super(DefaultErrorCode.getEnumEntry(DefaultErrorCode.createBadRequest(errorCodeEnum)), message, cause);
    }

    public BadRequestException(ErrorCodeEnum errorCodeEnum, Throwable cause) {
        super(DefaultErrorCode.getEnumEntry(DefaultErrorCode.createBadRequest(errorCodeEnum)), "", cause);
    }
}

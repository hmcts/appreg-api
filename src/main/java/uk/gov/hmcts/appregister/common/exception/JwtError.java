package uk.gov.hmcts.appregister.common.exception;

import org.springframework.http.HttpStatus;

public enum JwtError implements ErrorCodeEnum {
    MISSING_CLAIMS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Not all claims are present in the token", "JWT-1"));

    private final DefaultErrorDetail defaultErrorCode;

    JwtError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

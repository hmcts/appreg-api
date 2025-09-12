package uk.gov.hmcts.appregister.applicationfee.service.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCode;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;

public enum ApplicationFeeCode implements ErrorCodeEnum {
    AMBIGUOUS_FEE(
            DefaultErrorCode.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "To many fees returned", "FEE-1")),
    NO_MAIN_FEE(
            DefaultErrorCode.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No main fee returned", "FEE-2"));

    private final DefaultErrorCode defaultErrorCode;

    ApplicationFeeCode(DefaultErrorCode defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorCode getCode() {
        return defaultErrorCode;
    }
}

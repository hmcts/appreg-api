package uk.gov.hmcts.appregister.applicationfee.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ApplicationFeeCodeError implements ErrorCodeEnum {
    AMBIGUOUS_FEE(
            DefaultErrorDetail.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "To many fees returned", "FEE-1")),
    NO_MAIN_FEE(
            DefaultErrorDetail.create(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No main fee returned", "FEE-2"));

    private final DefaultErrorDetail defaultErrorCode;

    ApplicationFeeCodeError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

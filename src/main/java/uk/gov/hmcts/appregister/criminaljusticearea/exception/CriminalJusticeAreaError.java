package uk.gov.hmcts.appregister.criminaljusticearea.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum CriminalJusticeAreaError implements ErrorCodeEnum {
    CODE_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Criminal Justice Area not found", "CJA-1"));
    private final DefaultErrorDetail defaultErrorCode;

    CriminalJusticeAreaError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

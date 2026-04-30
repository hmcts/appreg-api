package uk.gov.hmcts.appregister.report.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ReportError implements ErrorCodeEnum {
    INVALID_LOCATION_COMBINATION(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Invalid report location filter combination",
                    "REPORT-1"));

    private final DefaultErrorDetail defaultErrorCode;

    ReportError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

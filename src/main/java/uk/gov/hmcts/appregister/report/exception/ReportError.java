package uk.gov.hmcts.appregister.report.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ReportError implements ErrorCodeEnum {
    CJA_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Criminal Justice Area not found", "RPT-1")),
    COURT_NOT_FOUND(DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Court not found", "RPT-2")),
    DUPLICATE_CJA_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Criminal Justice Areas found when only one was expected",
                    "RPT-3")),
    INVALID_LOCATION_COMBINATION(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Invalid report location combination", "RPT-4"));

    private final DefaultErrorDetail defaultErrorCode;

    ReportError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

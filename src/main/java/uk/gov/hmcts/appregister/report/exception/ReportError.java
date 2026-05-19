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
    COURT_SUPPLIED_WITH_OTHER_LOCATION_OR_CJA(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Court location code cannot be combined with other location filters",
                    "RPT-4")),
    OTHER_LOCATION_SUPPLIED_WITHOUT_CJA(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Other location description cannot be combined with CJA code",
                    "RPT-5")),
    INVALID_LOCATION_COMBINATION(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Either 'courtLocation' must be provided, or both 'criminalJusticeArea'"
                            + " and 'otherLocationDescription' must be supplied.",
                    "RPT-4"));

    private final DefaultErrorDetail defaultErrorCode;

    ReportError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

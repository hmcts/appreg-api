package uk.gov.hmcts.appregister.criminaljusticearea.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum CriminalJusticeAreaError implements ErrorCodeEnum {

    /**
     * No active Criminal Justice Area was found for the requested code.
     *
     * <p>HTTP 404 Not Found <br>
     * Business code: CJA-1
     */
    CJA_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "Criminal Justice Area not found", "CJA-1")),

    /**
     * More than one Criminal Justice Area was found when only one was expected.
     *
     * <p>HTTP 409 Conflict <br>
     * Business code: CJA-2
     */
    DUPLICATE_CJA_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "Multiple Criminal Justice Areas found when only one was expected",
                    "CJA-2"));

    private final DefaultErrorDetail defaultErrorCode;

    CriminalJusticeAreaError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

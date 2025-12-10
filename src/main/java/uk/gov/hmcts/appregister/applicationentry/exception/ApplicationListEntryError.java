package uk.gov.hmcts.appregister.applicationentry.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ApplicationListEntryError implements ErrorCodeEnum {
    LIST_ENTRY_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "No application list entry was found that belongs to" + "the specified list",
                    "ALE-1"));
    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListEntryError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

package uk.gov.hmcts.appregister.applicationentryresult.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

public enum ApplicationListEntryResultError implements ErrorCodeEnum {
    LIST_ENTRY_RESULT_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "No application list entry result was found that belongs to the specified entry",
                    "ALER-1")),
    ENTRY_RESULT_LIST_NOT_FOUND(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST, "Application List not found", "ALER-2")),
    INVALID_ENTRY_RESULT_LIST_STATUS(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete the entry result because the list has a CLOSED 'status'",
                    "ALER-3")),
    APPLICATION_LIST_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT, "The application list does not exist", "ALER-4")),
    APPLICATION_LIST_STATE_IS_INCORRECT(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "The application list state is not suitable for an entry result operation",
                    "ALER-5")),
    APPLICATION_ENTRY_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "No application list entry exists that belongs to the specified list",
                    "ALER-6")),
    RESOLUTION_CODE_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_FOUND, "The result code does not exist", "ALER-7")),
    APPLICATION_ENTRY_RESULT_DOES_NOT_EXIST(
            DefaultErrorDetail.create(
                    HttpStatus.CONFLICT,
                    "No application list entry result exists that belongs to the specified list and entry",
                    "ALER-8"));
    private final DefaultErrorDetail defaultErrorCode;

    ApplicationListEntryResultError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

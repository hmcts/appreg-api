package uk.gov.hmcts.appregister.csds.ingress.exception;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.appregister.common.exception.DefaultErrorDetail;
import uk.gov.hmcts.appregister.common.exception.ErrorCodeEnum;
import uk.gov.hmcts.appregister.common.exception.ErrorDetail;

/**
 * CSDS ingest specific error codes.
 */
public enum CsdsIngestError implements ErrorCodeEnum {
    FILE_MISSING(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "CSDS ingest file must be provided and not empty",
                    "CSI-1")),
    INVALID_FILE_FORMAT(
            DefaultErrorDetail.create(
                    HttpStatus.BAD_REQUEST,
                    "CSDS ingest file must contain a JSON object with a records array",
                    "CSI-2")),
    INVALID_PROCESSOR(
            DefaultErrorDetail.create(HttpStatus.BAD_REQUEST, "Unknown CSDS processor", "CSI-3")),
    PROCESSOR_NOT_IMPLEMENTED(
            DefaultErrorDetail.create(
                    HttpStatus.NOT_IMPLEMENTED,
                    "The requested CSDS ingest processor is not implemented yet",
                    "CSI-4")),
    LOCKED(
            DefaultErrorDetail.create(
                    HttpStatus.LOCKED, "The CSDS ingest is already running", "CSI-5"));

    private final DefaultErrorDetail defaultErrorCode;

    CsdsIngestError(DefaultErrorDetail defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    @Override
    public ErrorDetail getCode() {
        return defaultErrorCode;
    }
}

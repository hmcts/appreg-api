package uk.gov.hmcts.appregister.csds.ingress.exception;

import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Marks a CSDS duplicate-record failure so expected duplicate diagnostics are not logged with a
 * stack trace.
 */
public class CsdsDuplicateRecordException extends AppRegistryException {
    private static final long serialVersionUID = 1L;

    public CsdsDuplicateRecordException(String detail) {
        super(CommonAppError.INTERNAL_SERVER_ERROR, detail);
    }
}

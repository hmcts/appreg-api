package uk.gov.hmcts.appregister.audit.event;

import lombok.Getter;

/**
 * Describes the audit status of the underlying operation.
 */
@Getter
public enum OperationStatus {
    STARTED(1),
    COMPLETED(10),
    FAILED(-1);

    private int status;

    OperationStatus(int status) {
        this.status = status;
    }
}

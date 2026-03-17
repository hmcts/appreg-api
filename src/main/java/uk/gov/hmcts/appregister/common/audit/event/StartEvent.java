package uk.gov.hmcts.appregister.common.audit.event;

import uk.gov.hmcts.appregister.common.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * Represents the start of an operation audit event.
 */
public class StartEvent extends AuditEvent {
    public StartEvent(AuditOperation requestAction, String messageUuid, Keyable oldValue) {
        super(requestAction, OperationStatus.STARTED, NO_VALUE, messageUuid, null, oldValue);
    }
}

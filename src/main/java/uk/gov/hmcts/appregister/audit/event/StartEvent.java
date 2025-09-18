package uk.gov.hmcts.appregister.audit.event;

import uk.gov.hmcts.appregister.audit.AuditEventEnum;

/** Represents the start of an operation audit event. */
public class StartEvent extends AuditEvent {
    public StartEvent(AuditEventEnum requestAction, String messageUuid) {
        super(requestAction, OperationStatus.STARTED, NO_VALUE, messageUuid);
    }
}

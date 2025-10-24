package uk.gov.hmcts.appregister.audit.event;

import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.Optional;

/**
 * Represents the start of an operation audit event.
 */
public class StartEvent extends AuditEvent {
    public StartEvent(AuditOperation requestAction, String messageUuid, Optional<Keyable> oldValue, Optional<Keyable> newValue) {
        super(requestAction, OperationStatus.STARTED, NO_VALUE, messageUuid, newValue, oldValue);
    }
}

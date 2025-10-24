package uk.gov.hmcts.appregister.audit.event;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

import java.util.Optional;

/**
 * Represents a completed operation audit event.
 */
public class CompleteEvent extends AuditEvent {
    public CompleteEvent(BaseAuditEvent event, String response, Optional<Keyable> newEntity) {
        super(event);
        this.messageContent = response == null ? NO_VALUE : response;
        this.messageStatus = OperationStatus.COMPLETED;
        this.newValue = newEntity;
    }
}

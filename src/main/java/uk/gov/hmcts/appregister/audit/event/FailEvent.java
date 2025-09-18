package uk.gov.hmcts.appregister.audit.event;

/** Represents a failed operation audit event. */
public class FailEvent extends AuditEvent {
    public FailEvent(BaseAuditEvent event) {
        super(event);
        this.messageContent = NO_VALUE;
        this.messageStatus = OperationStatus.FAILED;
    }
}

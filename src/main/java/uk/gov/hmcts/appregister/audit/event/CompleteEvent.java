package uk.gov.hmcts.appregister.audit.event;

/** Represents a completed operation audit event. */
public class CompleteEvent extends AuditEvent {
    public CompleteEvent(BaseAuditEvent event, String response) {
        super(event);
        this.messageContent = response == null ? NO_VALUE : response;
        this.messageStatus = OperationStatus.COMPLETED;
    }
}

package uk.gov.hmcts.appregister.audit.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;

/** Encapsulates the audit request and response. */
@Getter
@AllArgsConstructor
@ToString
public class AuditEvent implements BaseAuditEvent {

    protected AuditEventEnum requestAction;

    protected OperationStatus messageStatus;

    protected String messageContent;

    protected String messageUuid;

    AuditEvent(BaseAuditEvent baseAuditEvent) {
        requestAction = baseAuditEvent.getRequestAction();
        messageContent = baseAuditEvent.getMessageContent();
        messageStatus = baseAuditEvent.getMessageStatus();
        messageUuid = baseAuditEvent.getMessageUuid();
    }
}

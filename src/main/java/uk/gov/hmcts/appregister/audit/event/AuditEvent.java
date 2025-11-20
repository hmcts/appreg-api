package uk.gov.hmcts.appregister.audit.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * Encapsulates the audit request and response.
 */
@Getter
@AllArgsConstructor
@ToString
public class AuditEvent implements BaseAuditEvent {

    protected AuditOperation requestAction;

    protected OperationStatus messageStatus;

    protected String messageContent;

    protected String messageUuid;

    protected Keyable newValue;

    protected Keyable oldValue;

    AuditEvent(BaseAuditEvent baseAuditEvent) {
        requestAction = baseAuditEvent.getRequestAction();
        messageContent = baseAuditEvent.getMessageContent();
        messageStatus = baseAuditEvent.getMessageStatus();
        messageUuid = baseAuditEvent.getMessageUuid();
        newValue = baseAuditEvent.getNewValue();
        oldValue = baseAuditEvent.getOldValue();
    }
}

package uk.gov.hmcts.appregister.common.audit.event;

import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * Represents a completed operation audit event.
 */
public class CompleteEvent extends AuditEvent {
    public CompleteEvent(BaseAuditEvent event, String response, Keyable newEntity) {
        super(event);
        this.messageContent = response == null ? NO_VALUE : response;
        this.messageStatus = OperationStatus.COMPLETED;
        this.newValue = newEntity;
    }

    /**
     * gets an indication of whether we are dealing with old, new or both values inside the event.
     *
     * @return What values do we have in the event
     */
    public AuditOldNewEnum getNewOldAuditState() {
        AuditOldNewEnum newOrOld =
                getOldValue() != null ? AuditOldNewEnum.OLD : AuditOldNewEnum.NEW;
        return getOldValue() != null && getNewValue() != null ? AuditOldNewEnum.BOTH : newOrOld;
    }
}

package uk.gov.hmcts.appregister.audit.event;

import uk.gov.hmcts.appregister.audit.AuditEventEnum;

/** The audit payload base interface. */
public interface BaseAuditEvent {
    String NO_VALUE = "NULL";

    /**
     * gets the request action.
     *
     * @return the request action
     */
    AuditEventEnum getRequestAction();

    /**
     * gets the message uuid.
     *
     * @return the message uuid
     */
    String getMessageUuid();

    /**
     * gets the message status.
     *
     * @return the message status
     */
    OperationStatus getMessageStatus();

    /**
     * gets the message content.
     *
     * @return the message content
     */
    String getMessageContent();
}

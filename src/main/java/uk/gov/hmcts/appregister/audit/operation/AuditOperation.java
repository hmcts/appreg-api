package uk.gov.hmcts.appregister.audit.operation;

import uk.gov.hmcts.appregister.common.enumeration.CRUDEnum;

/**
 * Represents an audit operation with an event name and type.
 */
public interface AuditOperation {
    String getEventName();

    CRUDEnum getType();
}

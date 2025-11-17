package uk.gov.hmcts.appregister.audit.operation;

import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

/**
 * Represents an audit operation with an event name and type.
 */
public interface AuditOperation {

    /** The audit event name of the operation. */
    String getEventName();

    /** The underlying audit operation. */
    CrudEnum getType();
}

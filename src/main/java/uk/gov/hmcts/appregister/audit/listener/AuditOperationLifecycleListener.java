package uk.gov.hmcts.appregister.audit.listener;

import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;

/** Audit logger listener for various audit events surrrounding an operation. */
@FunctionalInterface
public interface AuditOperationLifecycleListener {

    /**
     * Handles the given audit event.
     *
     * @param event the audit event to be handled
     */
    void eventPerformed(BaseAuditEvent event);
}

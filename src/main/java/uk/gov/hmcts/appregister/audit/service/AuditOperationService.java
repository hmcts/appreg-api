package uk.gov.hmcts.appregister.audit.service;

import java.util.Optional;
import java.util.function.Function;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;

/** An API that can be used to audit an event across all endpoints. */
public interface AuditOperationService {
    /**
     * process a command within the context of the audit.
     *
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     * @param listener The listeners that get executed in the order they are passed
     */
    <T> T processAudit(
            AuditEventEnum auditType,
            Function<BaseAuditEvent, Optional<T>> execution,
            AuditOperationLifecycleListener... listener);
}

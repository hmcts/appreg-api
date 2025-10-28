package uk.gov.hmcts.appregister.audit.service;

import java.util.Optional;
import java.util.function.Function;

import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * An API that can be used to audit an event across all endpoints.
 */
public interface AuditOperationService {
    /**
     * process a command within the context of the audit.
     * @param oldValue The old value before the operation is executed. This could be null if working with a
     *                 get or create operation etc
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     * @param listener The listeners that get executed in the order they are passed
     */
    <T, E extends Keyable> T processAudit(
            Optional<E> oldValue,
            AuditOperation<E> auditType,
            Function<BaseAuditEvent, Optional<AuditResult<T, E>>> execution,
            AuditOperationLifecycleListener... listener);
}

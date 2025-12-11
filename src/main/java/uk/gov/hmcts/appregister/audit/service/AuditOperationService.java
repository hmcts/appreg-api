package uk.gov.hmcts.appregister.audit.service;

import java.util.Optional;
import java.util.function.Function;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;

/**
 * An API that can be used to audit an event across all endpoints.
 */
public interface AuditOperationService {
    /**
     * process a command within the context of the audit.
     *
     * @param oldValue The old value before the operation is executed. Use {{@link
     *     #processAudit(uk.gov.hmcts.appregister.audit.operation.AuditOperation,
     *     java.util.function.Function, uk.gov.hmcts.appregister.audit.listener
     *     .AuditOperationLifecycleListener...)}} if no value is required
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     * @param listener The listeners that get executed in the order they are passed
     */
    <T, E extends Keyable> T processAudit(
            E oldValue,
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
            AuditOperationLifecycleListener... listener);

    /**
     * process a command within the context of the audit. The call assumes no old value so this
     * would be applicable for the GET or CREATE operations.
     *
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     * @param listener The listeners that get executed in the order they are passed
     */
    <T, E extends Keyable> T processAudit(
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
            AuditOperationLifecycleListener... listener);

    /**
     * process a command within the context of the audit.
     *
     * @param oldValue The old value before the operation is executed. Use {{@link
     *     #processAudit(uk.gov.hmcts.appregister.audit.operation.AuditOperation,
     *     java.util.function.Function, uk.gov.hmcts.appregister.audit.listener
     *     .AuditOperationLifecycleListener...)}} if no value is required
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     */
    <T, E extends Keyable> T processAudit(
            E oldValue,
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution);

    /**
     * process a command within the context of the audit. The call assumes no old value so this
     * would be applicable for the GET or CREATE operations.
     *
     * @param auditType The audit operation that will be applied to the request action
     * @param execution The function to execute. Assumes that this represents the input and output
     *     of the operation
     */
    <T, E extends Keyable> T processAudit(
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution);
}

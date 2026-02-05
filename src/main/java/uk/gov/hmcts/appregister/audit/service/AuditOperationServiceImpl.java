package uk.gov.hmcts.appregister.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.event.AuditEvent;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;
import uk.gov.hmcts.appregister.audit.model.AuditableResult;
import uk.gov.hmcts.appregister.audit.operation.AuditOperation;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;
import uk.gov.hmcts.appregister.common.exception.CommonAppError;

/**
 * Encapsulates a unit of work for the lifecycle of an auditable operation. Behaviour of each audit
 * lifecycle event can be controlled by passing in {@link
 * uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener} that will be invoked at
 * the start and end of the operation.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuditOperationServiceImpl implements AuditOperationService {

    /** The trace id name that is inserted by micrometer. */
    public static final String TRACE_ID = "traceId";

    private final ObjectMapper mapper;

    private final List<AuditOperationLifecycleListener> auditLifecycleListeners;

    @Override
    public <T, E extends Keyable> T processAudit(
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
        return processAudit(
                null,
                auditType,
                execution,
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public <T, E extends Keyable> T processAudit(
            E oldValue,
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution) {
        return processAudit(
                oldValue,
                auditType,
                execution,
                auditLifecycleListeners.toArray(new AuditOperationLifecycleListener[0]));
    }

    @Override
    public <T, E extends Keyable> T processAudit(
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
            AuditOperationLifecycleListener... listener) {
        return processAudit(null, auditType, execution, listener);
    }

    @Override
    public <T, E extends Keyable> T processAudit(
            E oldValue,
            AuditOperation auditType,
            Function<BaseAuditEvent, Optional<AuditableResult<T, E>>> execution,
            AuditOperationLifecycleListener... listener) {
        StartEvent event = new StartEvent(auditType, getTraceId(), oldValue);

        // before execution hook
        fireAuditEvent(event, listener);

        log.debug("Processed start of auditable operation: {}", auditType.getEventName());
        Optional<AuditableResult<T, E>> responsePayload;
        try {
            responsePayload = execution.apply(event);

            // check is the result fits the expectations according to the operation being performed
            checkIfAuditOperationIsSuitableForResult(auditType, oldValue, responsePayload);

            if (responsePayload.isPresent()) {
                // fire after the completed operation
                fireAuditEvent(
                        new CompleteEvent(
                                event,
                                responsePayload.get().getResultingValue() != null
                                        ? getBodyAsString(responsePayload.get().getResultingValue())
                                        : null,
                                responsePayload.get().getNewEntity()),
                        listener);
            } else {
                // fire after the completed operation
                fireAuditEvent(new CompleteEvent(event, null, null), listener);
            }

            log.debug("Processed success auditable operation: {}", auditType.getEventName());
        } catch (Exception e) {
            // fire after the failure of an operation
            fireAuditEvent(new FailEvent(event), listener);

            log.debug("Processed failure auditable operation: {}", auditType.getEventName());
            throw e;
        }

        return responsePayload.map(AuditableResult::getResultingValue).orElse(null);
    }

    /**
     * validates the audit operation is suitable for the old and new values being audited. Incorrect
     * usage throws an exception back to the user, this error is simply a programmatic error of the
     * audit api
     *
     * @param eventEnum The event type
     * @param result The result containing old and new values on which to audit
     */
    private <T, E extends Keyable> void checkIfAuditOperationIsSuitableForResult(
            AuditOperation eventEnum, E oldValue, Optional<AuditableResult<T, E>> result) {
        if (eventEnum.getType().isCreate()
                && ((oldValue != null)
                        || (result.isPresent() && result.get().getNewEntity() == null))) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR, "Create audit cannot have old entity");
        } else if (eventEnum.getType().isUpdate()
                && (!result.isPresent()
                        || (result.get().getNewEntity() == null || oldValue == null))) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR, "Update audit must have old and new");
        } else if (eventEnum.getType().isDelete() && oldValue == null) {
            throw new AppRegistryException(
                    CommonAppError.INTERNAL_SERVER_ERROR, "Delete audit must have old");
        }
    }

    /**
     * gets the json body in string form that will be part of the audit response.
     *
     * @return The body as a string or defaulted on an marshalling error
     */
    private String getBodyAsString(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error("Problem marshalling the json response for auditing", e);
            return "Problem marshalling the json response for auditing";
        }
    }

    /**
     * fires the audit event for an operation.
     *
     * @param listener The listener to fire with the event
     * @param auditEvent The audit event to fire
     */
    private void fireAuditEvent(
            AuditEvent auditEvent, AuditOperationLifecycleListener... listener) {
        for (AuditOperationLifecycleListener l : listener) {
            l.eventPerformed(auditEvent);
        }
    }

    /**
     * gets the trace id from the parsed Sleuth logging value.
     *
     * @return The trace id or a default message if not found
     */
    public static String getTraceId() {
        try {
            String traceId = MDC.get(TRACE_ID);
            if (traceId != null) {
                return MDC.get(TRACE_ID);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Couldn't find the trace id defaulting", e);
        }

        return "No Correlation Id Found";
    }
}

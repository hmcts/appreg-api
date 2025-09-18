package uk.gov.hmcts.appregister.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.audit.AuditEventEnum;
import uk.gov.hmcts.appregister.audit.event.AuditEvent;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationLifecycleListener;

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
    private static final String TRACE_ID = "traceId";

    private final ObjectMapper mapper;

    @Override
    public <T> T processAudit(
            AuditEventEnum auditType,
            Function<BaseAuditEvent, Optional<T>> execution,
            AuditOperationLifecycleListener... listener) {
        StartEvent event = new StartEvent(auditType, getTraceId());

        // before execution hook
        fireAuditEvent(event, listener);

        log.debug("Processed start of auditable operation: {}", event);
        Optional<T> responsePayload;
        try {
            responsePayload = execution.apply(event);
            if (responsePayload.isPresent()) {
                // fire after the completed operation
                fireAuditEvent(
                        new CompleteEvent(event, getBodyAsString(responsePayload.get())), listener);
            } else {
                // fire after the completed operation
                fireAuditEvent(new CompleteEvent(event, null), listener);
            }

            log.debug("Processed success auditable operation: {}", event);
        } catch (Exception e) {
            // fire after the failure of an operation
            fireAuditEvent(new FailEvent(event), listener);

            log.debug("Processed failure auditable operation: {}", event);
            throw e;
        }

        return responsePayload.orElse(null);
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
    protected String getTraceId() {
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

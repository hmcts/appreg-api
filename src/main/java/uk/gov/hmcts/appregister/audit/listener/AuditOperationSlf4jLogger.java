package uk.gov.hmcts.appregister.audit.listener;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;

/**
 * Logs the request and response of audit events using SLF4J.
 */
@Slf4j
public class AuditOperationSlf4jLogger extends AuditOperationLifecycleListenerAdapter {

    /** A prefix when starting an audit event. */
    public static final String START_AUDIT_LOG = "Start audit";

    /** A suffix when ending an audit event. */
    public static final String COMPLETION_AUDIT_LOG = "Completion audit";

    /** A suffix when failing an audit event. */
    public static final String FAILED_CFOMPLETION_AUDIT_LOG = "Completion fail audit";

    /** The message uuid. */
    private static final String MESSAGE_UUID = "p_messageuuid";

    /** The message status. */
    private static final String STATUS = "p_messagestatus";

    /** The message content. */
    private static final String CONTENT = "p_messagecontent";

    /** The action. */
    private static final String ACTION = "p_requestaction";

    @Override
    protected  void started(StartEvent request) {
        log.info("%s {}".formatted(START_AUDIT_LOG), getLog(request));
    }

    @Override
    protected void finished(CompleteEvent request) {
        log.info("%s {}".formatted(COMPLETION_AUDIT_LOG), getLog(request));
    }

    @Override
    protected void finishFail(FailEvent request) {
        log.info("%s {}".formatted(FAILED_CFOMPLETION_AUDIT_LOG), getLog(request));
    }

    public static String getLog(BaseAuditEvent event) {
        return "\n"
                + "-"
                + ACTION
                + "="
                + event.getRequestAction().getEventName()
                + "\n"
                + "-"
                + MESSAGE_UUID
                + "="
                + event.getMessageUuid()
                + "\n"
                + "-"
                + STATUS
                + "="
                + event.getMessageStatus().getStatus()
                + "\n"
                + "-"
                + CONTENT
                + "="
                + event.getMessageContent();
    }
}

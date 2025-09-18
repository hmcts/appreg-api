package uk.gov.hmcts.appregister.audit.listener;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.appregister.audit.event.BaseAuditEvent;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;

/** Logs the request and response of audit events using SLF4J. */
@Slf4j
public class AuditOperationSlf4jLogger extends AuditOperationLifecycleListenerAdapter {

    /** The message uuid. */
    private static final String MESSAGE_UUID = "p_messageuuid";

    /** The message status. */
    private static final String STATUS = "p_messagestatus";

    /** The message content. */
    private static final String CONTENT = "p_messagecontent";

    /** The action. */
    private static final String ACTION = "p_requestaction";

    @Override
    protected void started(StartEvent request) {
        log.info("Start audit {}", getLog(request));
    }

    @Override
    protected void finished(CompleteEvent request) {
        log.info("Completion audit {}", getLog(request));
    }

    @Override
    protected void finishFail(FailEvent request) {
        log.info("Completion fail audit {}", getLog(request));
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

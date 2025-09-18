package uk.gov.hmcts.appregister.audit;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

public class AuditOperationSlf4jLoggerTest {

    private LogCaptor logCaptor;

    @BeforeEach
    public void before() {
        logCaptor = LogCaptor.forClass(AuditOperationSlf4jLogger.class);
        logCaptor.clearLogs();
    }

    @Test
    public void testFailOperationLog() throws Exception {
        StartEvent startEvent =
                new StartEvent(AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT, "ID");
        FailEvent auditRequest = new FailEvent(startEvent);

        new AuditOperationSlf4jLogger().eventPerformed(auditRequest);

        Assertions.assertEquals(
                "Completion fail audit\s" + AuditOperationSlf4jLogger.getLog(auditRequest),
                logCaptor.getInfoLogs().getFirst());
    }

    @Test
    public void testBeforeOperationLog() throws Exception {
        StartEvent startEvent =
                new StartEvent(AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT, "ID");

        new AuditOperationSlf4jLogger().eventPerformed(startEvent);

        Assertions.assertEquals(
                "Start audit\s" + AuditOperationSlf4jLogger.getLog(startEvent),
                logCaptor.getInfoLogs().getFirst());
    }

    @Test
    public void testCompletedOperationLog() throws Exception {
        StartEvent startEvent =
                new StartEvent(AuditEventEnum.GET_APPLICATION_CODES_AUDIT_EVENT, "ID");
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null);

        new AuditOperationSlf4jLogger().eventPerformed(auditRequest);

        Assertions.assertEquals(
                "Completion audit\s" + AuditOperationSlf4jLogger.getLog(auditRequest),
                logCaptor.getInfoLogs().getFirst());
    }
}

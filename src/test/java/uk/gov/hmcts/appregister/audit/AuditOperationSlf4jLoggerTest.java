package uk.gov.hmcts.appregister.audit;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.applicationcode.audit.AppCodeAuditOperation;
import uk.gov.hmcts.appregister.audit.event.CompleteEvent;
import uk.gov.hmcts.appregister.audit.event.FailEvent;
import uk.gov.hmcts.appregister.audit.event.StartEvent;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

class AuditOperationSlf4jLoggerTest {

    private LogCaptor logCaptor;

    @BeforeEach
    void before() {
        logCaptor = LogCaptor.forClass(AuditOperationSlf4jLogger.class);
        logCaptor.clearLogs();
    }

    @Test
    void testFailOperationLog() {
        StartEvent startEvent =
                new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT, "ID", null);
        FailEvent auditRequest = new FailEvent(startEvent);

        new AuditOperationSlf4jLogger().eventPerformed(auditRequest);

        Assertions.assertEquals(
                "Completion fail audit\s" + AuditOperationSlf4jLogger.getLog(auditRequest),
                logCaptor.getInfoLogs().getFirst());
    }

    @Test
    void testBeforeOperationLog() {
        StartEvent startEvent =
                new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT, "ID", null);

        new AuditOperationSlf4jLogger().eventPerformed(startEvent);

        Assertions.assertEquals(
                "Start audit\s" + AuditOperationSlf4jLogger.getLog(startEvent),
                logCaptor.getInfoLogs().getFirst());
    }

    @Test
    void testCompletedOperationLog() {
        StartEvent startEvent =
                new StartEvent(AppCodeAuditOperation.GET_APPLICATION_CODES_AUDIT_EVENT, "ID", null);
        CompleteEvent auditRequest = new CompleteEvent(startEvent, null, null);

        new AuditOperationSlf4jLogger().eventPerformed(auditRequest);

        Assertions.assertEquals(
                "Completion audit\s" + AuditOperationSlf4jLogger.getLog(auditRequest),
                logCaptor.getInfoLogs().getFirst());
    }
}

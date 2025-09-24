package uk.gov.hmcts.appregister.testutils.util;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.audit.event.OperationStatus;
import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

/** A utility for making assertions around audit logs. */
public class AuditAssertUtil {

    /**
     * assert that a start log entry matches the expected format.
     *
     * @param action The action audit we expect
     * @param actualLogMessage The log message
     */
    public static void assertStart(String action, String actualLogMessage) {
        assertLogEntry(
                AuditOperationSlf4jLogger.START_AUDIT_LOG,
                action,
                OperationStatus.STARTED,
                actualLogMessage);
    }

    /**
     * assert that a completion log entry matches the expected format.
     *
     * @param action The action audit we expect
     * @param actualLogMessage The log message
     */
    public static void assertCompleted(String action, String actualLogMessage) {
        assertLogEntry(
                AuditOperationSlf4jLogger.COMPLETION_AUDIT_LOG,
                action,
                OperationStatus.COMPLETED,
                actualLogMessage);
    }

    /**
     * assert that a fail completion log entry matches the expected format.
     *
     * @param action The action audit we expect
     * @param actualLogMessage The log message
     */
    public static void assertFailCompleted(String action, String actualLogMessage) {
        assertLogEntry(
                AuditOperationSlf4jLogger.FAILED_CFOMPLETION_AUDIT_LOG,
                action,
                OperationStatus.FAILED,
                actualLogMessage);
    }

    /**
     * assert that a log entry matches the expected format.
     *
     * @param event The event we expect
     * @param action The action audit we expect
     * @param operationStatus The operation status we expect
     * @param actualLogMessage The log message
     */
    public static void assertLogEntry(
            String event, String action, OperationStatus operationStatus, String actualLogMessage) {
        Assertions.assertTrue(
                Pattern.matches(
                        ("%s\\s*-p_requestaction=%s\\R-p_messageuuid=.*"
                                        + "\\R-p_messagestatus=%s\\R-p_messagecontent=.*")
                                .formatted(event, action, operationStatus.getStatus()),
                        actualLogMessage));
    }
}

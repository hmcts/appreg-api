package uk.gov.hmcts.appregister.testutils.util;

import nl.altindag.log.LogCaptor;

import org.junit.jupiter.api.Assertions;

import uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger;

import java.util.regex.Pattern;

/**
 * Allows us to make assertions around activity/operation audit logs. This assertion class assumes
 * logs were written using the default @{AuditOperationSlf4jLogger} listener.
 */
public class ActivityAuditLogAsserter {
    protected final LogCaptor operationLogger =
        LogCaptor.forClass(uk.gov.hmcts.appregister.audit.listener.AuditOperationSlf4jLogger.class);

    /**
     * Asserts the activity log contains the expected values.
     * @param action The action
     * @param messageId The message id
     * @param messageStatus The message status
     * @param messageContent The message content
     */
    public void assertCompletedLogContains(String action, String messageId,
                                           String messageStatus,
                                           String messageContent) {

        String completed =
            AuditOperationSlf4jLogger.getCompletedLog(action, messageId, messageStatus,
                                                      messageContent);
        boolean asserted = false;
        for (String logs: operationLogger.getInfoLogs()) {
            if (logs.equals(completed)) {
                asserted = true;
            }
        }

        Assertions.assertTrue(asserted);
    }

    /**
     * Asserts the activity log contains the expected values with an unknown message id.
     * @param action The action
     * @param messageStatus The message status
     * @param messageContent The message content
     */
    public void assertCompletedLogContainsWithUnknownMessageId(String action,
                                           String messageStatus,
                                           String messageContent) {
        String completed =
            AuditOperationSlf4jLogger.getCompletedLogWithUnknownMessageIdRegEx(action, messageStatus,
                                                                               escapeRegex(messageContent));
        boolean asserted = false;
        for (String logs: operationLogger.getInfoLogs()) {
            if (Pattern.matches(completed, logs)) {
                asserted = true;
            }
        }

        Assertions.assertTrue(asserted);
    }

    /**
     * escapes regex.
     * @param input the input string regex
     * @return The escaped regex
     */
    protected String escapeRegex(String input) {
        return input.replaceAll(
            "[\\\\.^$|?*+()\\[\\]{}]",
            "\\\\$0"
        );
    }
}

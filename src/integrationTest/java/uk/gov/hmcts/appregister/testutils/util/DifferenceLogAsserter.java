package uk.gov.hmcts.appregister.testutils.util;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.audit.listener.diff.ReflectiveAuditDifferentiator;

/**
 * A class that allows us to read a log for differences. This class reads the logs from {@link
 * uk.gov.hmcts.appregister.audit.listener.DataAuditLogger} and {@link
 * ReflectiveAuditDifferentiator}.
 */
@Slf4j
@RequiredArgsConstructor
public class DifferenceLogAsserter {
    protected final LogCaptor dataAuditLogger =
            LogCaptor.forClass(uk.gov.hmcts.appregister.audit.listener.DataAuditLogger.class);

    protected final LogCaptor reflectiveDifferentiator =
            LogCaptor.forClass(ReflectiveAuditDifferentiator.class);

    private static final String DIFF_PREFIX = "Saving data audit difference:";

    private static final String DATA_RECORD = "Saved data audit entity:";

    /**
     * gets the data audit assertion strings.
     *
     * @param tableName The table name
     * @param columnName The column name
     * @param oldValue The old value
     * @param newValue The new value
     * @param updateType The audit type e.g. CREATE/UPDATE/DELETE
     * @param eventName The event name
     */
    public static DataAuditResult getDataAuditAssertion(
            String tableName,
            String columnName,
            String oldValue,
            String newValue,
            String updateType,
            String eventName) {

        if (oldValue == null) {
            oldValue = ".*";
        }

        if (newValue == null) {
            newValue = ".*";
        }

        return new DataAuditResult(
                getAssertionString(tableName, columnName, oldValue, newValue),
                String.format(
                        DATA_RECORD
                                + " DataAudit\\(id=.*,"
                                + " schemaName=appreg,"
                                + " tableName=%s,"
                                + " columnName=%s,"
                                + " oldValue=%s,"
                                + " newValue=%s,.*"
                                + " changedDate=.*,"
                                + " relatedKey=.*,"
                                + " updateType=%s.*"
                                + " eventName=%s,.*"
                                + " changedBy=.*\\)",
                        tableName,
                        columnName,
                        oldValue,
                        newValue,
                        updateType,
                        eventName));
    }

    /**
     * The log regex pattern of the {@link uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}.
     */
    private static final String DIFF_LOG_PATTERN =
            DIFF_PREFIX + " Difference\\(tableName=%s, fieldName=%s, oldValue=%s, newValue=%s\\)";

    /**
     * The string assertion. Failure on absence of the string.
     *
     * @param assertion The assertion to find
     */
    public void assertDifferenceOrDataAuditChange(DataAuditResult assertion) {

        boolean differenceFound = false;
        // check difference log exists
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (Pattern.matches(assertion.differenceRegex(), log)) {
                differenceFound = true;
            }
        }

        if (differenceFound) {
            // check the data audit record log exists
            for (String log : dataAuditLogger.getDebugLogs()) {
                if (Pattern.matches(assertion.dataAuditRegex(), log)) {
                    return;
                }
            }
        }

        throw new AssertionError(
                "Expected no differences, but found: " + dataAuditLogger.getErrorLogs());
    }

    public void assertFieldLogPresent(String fieldName) {
        String pattern =
                DIFF_PREFIX
                        + " Difference\\(tableName=.*, fieldName="
                        + fieldName
                        + ", oldValue=.*, newValue=.*\\)";
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (Pattern.matches(pattern, log)) {
                return;
            }
        }

        throw new AssertionError(
                "Expected not null difference for field: "
                        + fieldName
                        + ", but not found in logs: "
                        + dataAuditLogger.getDebugLogs());
    }

    /**
     * Assert that a log exists for the field name specified.
     *
     * @param fieldName The fieldname
     */
    public void assertFieldLogNotPresent(String fieldName) {
        try {
            assertFieldLogPresent(fieldName);
            Assertions.fail();
        } catch (AssertionError assertionError) {
            log.debug("Caught expected exception: {}", assertionError.getMessage());
        }
    }

    /**
     * The string that we need to assert against.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @param oldValue the old value
     * @param newValue The new value
     * @return The assertion string to detect differences
     */
    private static String getAssertionString(
            String tableName, String fieldName, String oldValue, String newValue) {
        return String.format(DIFF_LOG_PATTERN, tableName, fieldName, oldValue, newValue);
    }

    /**
     * clears the underlying logs from the {@link
     * uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}.
     */
    public void clearLogs() {
        dataAuditLogger.clearLogs();
    }

    /**
     * Asserts that no errors have happened. When we say errors we mean any log entries at WARN or
     * ERROR level.
     */
    public void assertNoErrors() {
        // assert the audit log message
        Assertions.assertEquals(0, dataAuditLogger.getWarnLogs().size());
        Assertions.assertEquals(0, dataAuditLogger.getErrorLogs().size());

        Assertions.assertEquals(0, reflectiveDifferentiator.getWarnLogs().size());
        Assertions.assertEquals(0, reflectiveDifferentiator.getErrorLogs().size());
    }

    /** assert a count of differences. */
    public void assertDiffCount(int assertCount) {
        int count = 0;
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (Pattern.matches(DIFF_PREFIX + ".*", log)) {
                return;
            }
        }

        Assertions.assertEquals(count, assertCount);
    }

    record DataAuditResult(String differenceRegex, String dataAuditRegex) {}
}

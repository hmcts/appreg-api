package uk.gov.hmcts.appregister.testutils.util;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.appregister.audit.listener.diff.ReflectiveAuditor;

/**
 * A class that allows us to assert against audit log data. This class reads the logs from {@link
 * uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}
 */
@Slf4j
@RequiredArgsConstructor
public class AuditLogAsserter {
    protected final LogCaptor dataAuditLogger =
            LogCaptor.forClass(uk.gov.hmcts.appregister.audit.listener.DataAuditLogger.class);

    protected final LogCaptor reflectiveDifferentiator =
            LogCaptor.forClass(ReflectiveAuditor.class);

    private static final String DIFF_NEW_PREFIX = "Saving data audit new:";
    private static final String DIFF_OLD_PREFIX = "Saving data audit old:";

    private static final String DATA_RECORD = "Saved data audit entity:";

    /**
     * gets the data audit assertion strings.
     *
     * @param tableName The table name
     * @param columnName The column name
     * @param oldValue The old value. An empty string means we assert on an old audit value but not a specific value.
     *                 A null means do not assert on old value at all
     * @param newValue The new value. An empty string means we assert on an new audit value but not a specific value.
     *                 A null means do not assert on new value at all
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

        if (oldValue!=null && oldValue.isEmpty()) {
            oldValue = ".*";
        }

        if (newValue!=null && newValue.isEmpty()) {
            newValue = ".*";
        }

        return new DataAuditResult(
                oldValue != null ? getOldAssertionString(tableName, columnName, oldValue) : null,
                newValue != null ? getNewAssertionString(tableName, columnName, newValue) : null,
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
                        oldValue != null ? oldValue : ".*",
                        newValue != null ? newValue : ".*",
                        updateType,
                        eventName));
    }

    /**
     * The log regex pattern of the {@link uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}.
     */
    private static final String DIFF_NEW_LOG_PATTERN =
            DIFF_NEW_PREFIX + " AuditableData\\(tableName=%s, fieldName=%s, value=%s\\)";

    /**
     * The log regex pattern of the {@link uk.gov.hmcts.appregister.audit.listener.DataAuditLogger}.
     */
    private static final String DIFF_OLD_LOG_PATTERN =
            DIFF_OLD_PREFIX + " AuditableData\\(tableName=%s, fieldName=%s, value=%s\\)";

    /**
     * The string assertion. Failure on absence of the string.
     *
     * @param assertion The assertion to find
     */
    public void assertDataAuditChange(DataAuditResult assertion) {

        // if we are not looking for old or new audi logs
        boolean oldLogFound = assertion.oldAuditRegex() == null;
        boolean newLogFound = assertion.newAuditRegex() == null;
        boolean auditLogFound = false;

        // find new audit log exists
        if (assertion.newAuditRegex() != null) {
            for (String log : dataAuditLogger.getDebugLogs()) {
                if (Pattern.matches(assertion.newAuditRegex(), log)) {
                    newLogFound = true;
                }
            }
        }

        // check if old audit log exists
        if (assertion.oldAuditRegex() != null){
            for (String log : dataAuditLogger.getDebugLogs()) {
                if (!Pattern.matches(assertion.oldAuditRegex(), log)) {
                    oldLogFound = true;
                }
            }
        }

        // check the data audit record log exists
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (Pattern.matches(assertion.dataAuditRegex(), log)) {
                auditLogFound = true;
            }
        }

        if (!oldLogFound || !newLogFound || !auditLogFound) {
            throw new AssertionError(
                    "We did not found expected logs");
        }
    }

    public void assertFieldLogPresent(String tableName, String fieldName, boolean newAudit) {
        String pattern =
                newAudit ? DIFF_NEW_PREFIX : DIFF_OLD_PREFIX
                        + " AuditableDifferenceData\\(tableName="
                        + tableName
                        + ", fieldName="
                        + fieldName
                        + ", value=.*\\)";
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (Pattern.matches(pattern, log)) {
                return;
            }
        }

        throw new AssertionError(
                "Expected not null audit for field: "
                        + fieldName
                        + ", but not found in logs: "
                        + dataAuditLogger.getDebugLogs());
    }

    /**
     * Assert that a log exists for the field name specified.
     * @param tableName The table name to find
     * @param fieldName The fieldname
     * @param newAudit Lookup for new audit records if true or false is old audit records
     */
    public void assertFieldLogNotPresent(String tableName, String fieldName, boolean newAudit) {
        try {
            assertFieldLogPresent(tableName, fieldName, newAudit);
            Assertions.fail();
        } catch (AssertionError assertionError) {
            log.debug("Caught expected exception: {}", assertionError.getMessage());
        }
    }

    /**
     * The string that we need to assert for a new value.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @param newValue The new value
     * @return The assertion string to detect audit logs
     */
    private static String getNewAssertionString(
            String tableName, String fieldName, String newValue) {
        return String.format(DIFF_NEW_LOG_PATTERN, tableName, fieldName, newValue);
    }

    /**
     * The string that we need to assert against for an old value.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @param oldValue The old value
     * @return The assertion string to detect audit logs
     */
    private static String getOldAssertionString(
            String tableName, String fieldName, String oldValue) {
        return String.format(DIFF_OLD_LOG_PATTERN, tableName, fieldName, oldValue);
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

    /**
     * assert a count for audit logs for either new or old audit logs
     * @param assertCount The assert count
     * @param newAudit The new audit records being looked for
     */
    public void assertDiffCount(int assertCount, boolean newAudit) {
        int count = 0;
        for (String log : dataAuditLogger.getDebugLogs()) {
            if (newAudit) {
                    if (Pattern.matches(getNewAssertionString(".*", ".*", ".*"), log)) {
                    count = count + 1;
                }
            } else {
                if (Pattern.matches(getOldAssertionString(".*", ".*", ".*"), log)) {
                    count = count + 1;
                }
            }
        }

        Assertions.assertEquals(count, assertCount);
    }

    record DataAuditResult(String oldAuditRegex, String newAuditRegex, String dataAuditRegex) {}
}

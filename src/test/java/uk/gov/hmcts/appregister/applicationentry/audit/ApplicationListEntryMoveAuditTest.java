package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class ApplicationListEntryMoveAuditTest {

    @Test
    void extractAuditData_returnsListIdAndVersionRowsForMoveAudit() {
        // Build the same minimal database-backed surrogate the move operation will pass into the
        // audit framework before and after reassigning the entry to a different list.
        val applicationList = new ApplicationList();
        applicationList.setId(321L);

        val applicationListEntry = new ApplicationListEntry();
        applicationListEntry.setId(123L);
        applicationListEntry.setApplicationList(applicationList);
        applicationListEntry.setVersion(7L);

        val audit = ApplicationListEntryMoveAudit.from(applicationListEntry);

        // Extract the field-level data exactly as the data-audit logger will see it for a move.
        val auditData = audit.extractAuditData(CrudEnum.UPDATE);

        Assertions.assertTrue(
                containsAuditRow(auditData, TableNames.APPLICATION_LISTS, "al_id", "321"));
        Assertions.assertTrue(
                containsAuditRow(auditData, TableNames.APPLICATION_LISTS_ENTRY, "version", "7"));
        Assertions.assertEquals(123L, audit.getId());
    }

    private boolean containsAuditRow(
            List<AuditableData> auditData, String tableName, String fieldName, String value) {
        return auditData.stream()
                .anyMatch(
                        row ->
                                tableName.equals(row.getTableName())
                                        && fieldName.equals(row.getFieldName())
                                        && value.equals(row.getValue()));
    }
}

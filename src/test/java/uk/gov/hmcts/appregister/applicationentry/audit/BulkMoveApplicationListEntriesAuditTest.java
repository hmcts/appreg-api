package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

class BulkMoveApplicationListEntriesAuditTest {

    @Test
    void extractAuditData_includesSourceTargetAndMovedEntries() {
        val sourceListId = 10L;
        val targetListId = 20L;
        val sourceListUuid = UUID.randomUUID();
        val targetListUuid = UUID.randomUUID();
        val firstEntry = entry(101L, UUID.randomUUID(), (short) 2, 7L);
        val secondEntry = entry(102L, UUID.randomUUID(), (short) 4, 8L);

        val audit =
                BulkMoveApplicationListEntriesAudit.forState(
                        sourceListId,
                        sourceListUuid,
                        targetListId,
                        targetListUuid,
                        List.of(firstEntry, secondEntry));

        val auditData = audit.extractAuditData(CrudEnum.UPDATE);

        Assertions.assertEquals(sourceListId, audit.getId());
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_move_source_list_uuid",
                        sourceListUuid.toString()));
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_move_target_list_uuid",
                        targetListUuid.toString()));
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_move_entry_count",
                        "2"));
        val movedEntriesValue = fieldValue(auditData, "bulk_move_entries");
        Assertions.assertNotNull(movedEntriesValue);
        Assertions.assertTrue(movedEntriesValue.contains(firstEntry.getUuid().toString()));
        Assertions.assertTrue(movedEntriesValue.contains(secondEntry.getUuid().toString()));
        Assertions.assertTrue(movedEntriesValue.contains("\"sequenceNumber\":2"));
        Assertions.assertTrue(movedEntriesValue.contains("\"sequenceNumber\":4"));
        Assertions.assertTrue(movedEntriesValue.contains("\"version\":7"));
        Assertions.assertTrue(movedEntriesValue.contains("\"version\":8"));
    }

    private ApplicationListEntry entry(Long id, UUID uuid, short sequenceNumber, Long version) {
        val entry = new ApplicationListEntry();
        entry.setId(id);
        entry.setUuid(uuid);
        entry.setSequenceNumber(sequenceNumber);
        entry.setVersion(version);
        return entry;
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

    private String fieldValue(List<AuditableData> auditData, String fieldName) {
        return auditData.stream()
                .filter(
                        row ->
                                TableNames.APPLICATION_LISTS_ENTRY.equals(row.getTableName())
                                        && fieldName.equals(row.getFieldName()))
                .map(AuditableData::getValue)
                .findFirst()
                .orElse(null);
    }
}

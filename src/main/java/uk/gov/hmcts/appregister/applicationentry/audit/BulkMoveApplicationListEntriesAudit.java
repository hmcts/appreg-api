package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

public record BulkMoveApplicationListEntriesAudit(
        Long sourceListId,
        UUID sourceListUuid,
        Long targetListId,
        UUID targetListUuid,
        int entryCount,
        String entries)
        implements Auditable {

    private static final String TABLE_NAME = TableNames.APPLICATION_LISTS_ENTRY;
    private static final String SOURCE_LIST_UUID_FIELD = "bulk_move_source_list_uuid";
    private static final String TARGET_LIST_UUID_FIELD = "bulk_move_target_list_uuid";
    private static final String ENTRY_COUNT_FIELD = "bulk_move_entry_count";
    private static final String ENTRIES_FIELD = "bulk_move_entries";

    @Override
    public Long getId() {
        return sourceListId;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(
                new AuditableData(TABLE_NAME, SOURCE_LIST_UUID_FIELD, sourceListUuid.toString()),
                new AuditableData(TABLE_NAME, TARGET_LIST_UUID_FIELD, targetListUuid.toString()),
                new AuditableData(TABLE_NAME, ENTRY_COUNT_FIELD, Integer.toString(entryCount)),
                new AuditableData(TABLE_NAME, ENTRIES_FIELD, entries));
    }

    public static BulkMoveApplicationListEntriesAudit forState(
            Long sourceListId,
            UUID sourceListUuid,
            Long targetListId,
            UUID targetListUuid,
            List<ApplicationListEntry> entries) {
        var sortedEntries =
                entries.stream()
                        .sorted(
                                Comparator.comparing(ApplicationListEntry::getSequenceNumber)
                                        .thenComparing(ApplicationListEntry::getId))
                        .toList();

        return new BulkMoveApplicationListEntriesAudit(
                sourceListId,
                sourceListUuid,
                targetListId,
                targetListUuid,
                sortedEntries.size(),
                formatEntries(sortedEntries));
    }

    private static String formatEntries(List<ApplicationListEntry> entries) {
        return entries.stream()
                .map(
                        entry ->
                                "{\"entryId\":\"%s\",\"sequenceNumber\":%s,\"version\":%s}"
                                        .formatted(
                                                entry.getUuid(),
                                                entry.getSequenceNumber(),
                                                entry.getVersion()))
                .collect(Collectors.joining(",", "[", "]"));
    }
}

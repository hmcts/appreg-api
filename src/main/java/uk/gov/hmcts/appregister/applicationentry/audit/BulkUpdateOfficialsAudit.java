package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.BulkAuditFormatting;
import uk.gov.hmcts.appregister.common.entity.AppListEntryOfficial;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.Official;

public record BulkUpdateOfficialsAudit(
        Long listId, UUID listUuid, List<UUID> entryIds, int entryCount, String officials)
        implements Auditable {

    private static final String TABLE_NAME = TableNames.APPLICATION_LISTS_ENTRY;
    private static final String LIST_UUID_FIELD = "bulk_officials_list_uuid";
    private static final String ENTRY_IDS_FIELD = "bulk_officials_entry_ids";
    private static final String ENTRY_COUNT_FIELD = "bulk_officials_entry_count";
    private static final String OFFICIALS_FIELD = "bulk_officials_officials";

    @Override
    public Long getId() {
        return listId;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(
                new AuditableData(TABLE_NAME, LIST_UUID_FIELD, listUuid.toString()),
                new AuditableData(
                        TABLE_NAME,
                        ENTRY_IDS_FIELD,
                        BulkAuditFormatting.formatSortedUuidArray(entryIds)),
                new AuditableData(TABLE_NAME, ENTRY_COUNT_FIELD, Integer.toString(entryCount)),
                new AuditableData(TABLE_NAME, OFFICIALS_FIELD, officials));
    }

    public static String formatDeletedOfficials(List<AppListEntryOfficial> deletedOfficials) {
        return deletedOfficials.stream()
                .sorted(
                        Comparator.comparing(
                                        (AppListEntryOfficial official) ->
                                                official.getAppListEntry().getSequenceNumber())
                                .thenComparing(
                                        AppListEntryOfficial::getId,
                                        Comparator.nullsLast(Long::compareTo)))
                .map(
                        official ->
                                ("{\"entryId\":\"%s\",\"sequenceNumber\":%s,\"type\":\"%s\","
                                                + "\"title\":\"%s\",\"forename\":\"%s\","
                                                + "\"surname\":\"%s\"}")
                                        .formatted(
                                                official.getAppListEntry().getUuid(),
                                                official.getAppListEntry().getSequenceNumber(),
                                                official.getOfficialType(),
                                                BulkAuditFormatting.escape(official.getTitle()),
                                                BulkAuditFormatting.escape(official.getForename()),
                                                BulkAuditFormatting.escape(official.getSurname())))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatReplacementOfficials(List<Official> replacementOfficials) {
        return replacementOfficials.stream()
                .map(
                        official ->
                                "{\"type\":\"%s\",\"title\":\"%s\",\"forename\":\"%s\",\"surname\":\"%s\"}"
                                        .formatted(
                                                official.getType(),
                                                BulkAuditFormatting.escape(official.getTitle()),
                                                BulkAuditFormatting.escape(official.getForename()),
                                                BulkAuditFormatting.escape(official.getSurname())))
                .collect(Collectors.joining(",", "[", "]"));
    }
}

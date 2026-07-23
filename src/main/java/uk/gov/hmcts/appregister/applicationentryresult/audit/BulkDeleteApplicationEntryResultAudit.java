package uk.gov.hmcts.appregister.applicationentryresult.audit;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.BulkAuditFormatting;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultItemDto;

public record BulkDeleteApplicationEntryResultAudit(
        Long resultId, List<UUID> listIds, List<UUID> entryIds, int resultCount, String results)
        implements Auditable {

    private static final String TABLE_NAME = TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS;
    private static final String LIST_IDS_FIELD = "bulk_delete_results_list_ids";
    private static final String ENTRY_IDS_FIELD = "bulk_delete_results_entry_ids";
    private static final String RESULT_COUNT_FIELD = "bulk_delete_results_count";
    private static final String RESULTS_FIELD = "bulk_delete_results";

    @Override
    public Long getId() {
        return resultId;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        return List.of(
                new AuditableData(
                        TABLE_NAME,
                        LIST_IDS_FIELD,
                        BulkAuditFormatting.formatSortedUuidArray(listIds)),
                new AuditableData(
                        TABLE_NAME,
                        ENTRY_IDS_FIELD,
                        BulkAuditFormatting.formatSortedUuidArray(entryIds)),
                new AuditableData(TABLE_NAME, RESULT_COUNT_FIELD, Integer.toString(resultCount)),
                new AuditableData(TABLE_NAME, RESULTS_FIELD, results));
    }

    public static String formatRequestedResults(List<BulkDeleteResultItemDto> requestedResults) {
        return requestedResults.stream()
                .map(
                        requestedResult ->
                                "{\"listId\":\"%s\",\"entryId\":\"%s\",\"resultId\":\"%s\"}"
                                        .formatted(
                                                requestedResult.getListId(),
                                                requestedResult.getEntryId(),
                                                requestedResult.getResultId()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatDeletedResults(List<AppListEntryResolution> deletedResults) {
        return deletedResults.stream()
                .sorted(
                        Comparator.comparing(
                                        (AppListEntryResolution resolution) ->
                                                resolution
                                                        .getApplicationList()
                                                        .getApplicationList()
                                                        .getId())
                                .thenComparing(
                                        resolution -> resolution.getApplicationList().getId())
                                .thenComparing(AppListEntryResolution::getId))
                .map(
                        resolution ->
                                ("{\"listId\":\"%s\",\"entryId\":\"%s\",\"resultId\":\"%s\","
                                                + "\"sequenceNumber\":%s,\"resultCode\":\"%s\","
                                                + "\"wording\":\"%s\",\"officer\":\"%s\"}")
                                        .formatted(
                                                resolution
                                                        .getApplicationList()
                                                        .getApplicationList()
                                                        .getUuid(),
                                                resolution.getApplicationList().getUuid(),
                                                resolution.getUuid(),
                                                resolution.getApplicationList().getSequenceNumber(),
                                                BulkAuditFormatting.escape(
                                                        resolution
                                                                .getResolutionCode()
                                                                .getResultCode()),
                                                BulkAuditFormatting.escape(
                                                        resolution.getResolutionWording()),
                                                BulkAuditFormatting.escape(
                                                        resolution.getResolutionOfficer())))
                .collect(Collectors.joining(",", "[", "]"));
    }
}

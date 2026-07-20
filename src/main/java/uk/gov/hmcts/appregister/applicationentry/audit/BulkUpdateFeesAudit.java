package uk.gov.hmcts.appregister.applicationentry.audit;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.hmcts.appregister.audit.listener.diff.Auditable;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.audit.listener.diff.BulkAuditFormatting;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;

public record BulkUpdateFeesAudit(
        Long listId,
        UUID listUuid,
        List<UUID> entryIds,
        int entryCount,
        String feeDetails,
        String offsiteEntryIds)
        implements Auditable {

    private static final String TABLE_NAME = TableNames.APPLICATION_LISTS_ENTRY;
    private static final String LIST_UUID_FIELD = "bulk_fees_list_uuid";
    private static final String ENTRY_IDS_FIELD = "bulk_fees_entry_ids";
    private static final String ENTRY_COUNT_FIELD = "bulk_fees_entry_count";
    private static final String FEE_DETAILS_FIELD = "bulk_fees_fee_details";
    private static final String OFFSITE_ENTRY_IDS_FIELD = "bulk_fees_offsite_entry_ids";

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
                new AuditableData(TABLE_NAME, FEE_DETAILS_FIELD, feeDetails),
                new AuditableData(TABLE_NAME, OFFSITE_ENTRY_IDS_FIELD, offsiteEntryIds));
    }

    public static String formatExistingFeeStatuses(List<AppListEntryFeeStatus> feeStatuses) {
        return feeStatuses.stream()
                .sorted(
                        Comparator.comparing(
                                        (AppListEntryFeeStatus status) ->
                                                status.getAppListEntry().getSequenceNumber())
                                .thenComparing(
                                        status -> status.getId(),
                                        Comparator.nullsLast(Long::compareTo)))
                .map(
                        status ->
                                ("{\"entryId\":\"%s\",\"status\":\"%s\",\"statusDate\":\"%s\","
                                                + "\"paymentReference\":\"%s\"}")
                                        .formatted(
                                                status.getAppListEntry().getUuid(),
                                                status.getAlefsFeeStatus(),
                                                status.getAlefsFeeStatusDate(),
                                                BulkAuditFormatting.escape(
                                                        status.getAlefsPaymentReference())))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatRequestedFeeDetails(List<BulkFeeDetailsDto> feeDetails) {
        return feeDetails.stream()
                .map(
                        feeDetail ->
                                ("{\"paymentStatus\":\"%s\",\"statusDate\":\"%s\","
                                                + "\"paymentReference\":\"%s\",\"hasOffsiteFee\":%s}")
                                        .formatted(
                                                feeDetail.getPaymentStatus(),
                                                feeDetail.getStatusDate(),
                                                BulkAuditFormatting.escape(
                                                        feeDetail.getPaymentReference()),
                                                Boolean.TRUE.equals(feeDetail.getHasOffsiteFee())))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatOffsiteEntryIds(
            Collection<Long> entryIds, Map<Long, UUID> entryUuidsById) {
        return entryIds.stream()
                .map(entryUuidsById::get)
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(), BulkAuditFormatting::formatSortedUuidArray));
    }
}

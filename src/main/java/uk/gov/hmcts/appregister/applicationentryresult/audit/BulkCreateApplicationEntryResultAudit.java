package uk.gov.hmcts.appregister.applicationentryresult.audit;

import java.util.ArrayList;
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
import uk.gov.hmcts.appregister.generated.model.TemplateSubstitution;

public record BulkCreateApplicationEntryResultAudit(
        Long anchorId,
        UUID listUuid,
        List<UUID> entryIds,
        int entryCount,
        String resultCode,
        String wordingFields,
        String createdResults)
        implements Auditable {

    private static final String TABLE_NAME = TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS;
    private static final String LIST_UUID_FIELD = "bulk_results_list_uuid";
    private static final String ENTRY_IDS_FIELD = "bulk_results_entry_ids";
    private static final String ENTRY_COUNT_FIELD = "bulk_results_entry_count";
    private static final String RESULT_CODE_FIELD = "bulk_results_result_code";
    private static final String WORDING_FIELDS_FIELD = "bulk_results_wording_fields";
    private static final String CREATED_RESULTS_FIELD = "bulk_results_created";

    @Override
    public Long getId() {
        return anchorId;
    }

    @Override
    public List<AuditableData> extractAuditData(CrudEnum crudEnum) {
        var auditData =
                new ArrayList<>(
                        List.of(
                                new AuditableData(
                                        TABLE_NAME,
                                        ENTRY_IDS_FIELD,
                                        BulkAuditFormatting.formatSortedUuidArray(entryIds)),
                                new AuditableData(
                                        TABLE_NAME,
                                        ENTRY_COUNT_FIELD,
                                        Integer.toString(entryCount)),
                                new AuditableData(TABLE_NAME, RESULT_CODE_FIELD, resultCode),
                                new AuditableData(TABLE_NAME, WORDING_FIELDS_FIELD, wordingFields),
                                new AuditableData(
                                        TABLE_NAME, CREATED_RESULTS_FIELD, createdResults)));

        if (listUuid != null) {
            auditData.add(new AuditableData(TABLE_NAME, LIST_UUID_FIELD, listUuid.toString()));
        }

        return auditData;
    }

    public static String formatWordingFields(List<TemplateSubstitution> wordingFields) {
        if (wordingFields == null || wordingFields.isEmpty()) {
            return "[]";
        }

        return wordingFields.stream()
                .map(
                        substitution ->
                                "{\"key\":\"%s\",\"value\":\"%s\"}"
                                        .formatted(
                                                BulkAuditFormatting.escape(substitution.getKey()),
                                                BulkAuditFormatting.escape(
                                                        substitution.getValue())))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatCreatedResults(List<AppListEntryResolution> createdResults) {
        return createdResults.stream()
                .sorted(
                        Comparator.comparing(
                                        (AppListEntryResolution resolution) ->
                                                resolution.getApplicationList().getSequenceNumber())
                                .thenComparing(
                                        AppListEntryResolution::getId,
                                        Comparator.nullsLast(Long::compareTo)))
                .map(
                        resolution ->
                                """
                                {"resultId":"%s","entryId":"%s","sequenceNumber":%s,
                                "resultCode":"%s","wording":"%s","officer":"%s"}
                                """
                                        .strip()
                                        .formatted(
                                                resolution.getUuid(),
                                                resolution.getApplicationList().getUuid(),
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

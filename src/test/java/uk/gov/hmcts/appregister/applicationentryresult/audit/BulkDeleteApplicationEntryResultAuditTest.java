package uk.gov.hmcts.appregister.applicationentryresult.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.entity.AppListEntryResolution;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.ResolutionCode;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.generated.model.BulkDeleteResultItemDto;

class BulkDeleteApplicationEntryResultAuditTest {

    @Test
    void extractAuditData_includesSummaryFields() {
        var listId = UUID.randomUUID();
        var entryId = UUID.randomUUID();
        var audit =
                new BulkDeleteApplicationEntryResultAudit(
                        10L, List.of(listId), List.of(entryId), 1, "[{\"resultId\":\"x\"}]");

        var auditData = audit.extractAuditData(CrudEnum.DELETE);

        assertThat(auditData)
                .anyMatch(
                        row ->
                                TableNames.APPLICATION_LIST_ENTRY_RESOLUTIONS.equals(
                                                row.getTableName())
                                        && "bulk_delete_results_count".equals(row.getFieldName())
                                        && "1".equals(row.getValue()));
    }

    @Test
    void formatHelpers_renderExpectedValues() {
        var requestedItem =
                new BulkDeleteResultItemDto()
                        .listId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .entryId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .resultId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        assertThat(
                        BulkDeleteApplicationEntryResultAudit.formatRequestedResults(
                                List.of(requestedItem)))
                .contains(
                        requestedItem.getListId().toString(),
                        requestedItem.getResultId().toString());

        var list = new ApplicationList();
        list.setId(1L);
        list.setUuid(requestedItem.getListId());
        var entry = new ApplicationListEntry();
        entry.setId(2L);
        entry.setUuid(requestedItem.getEntryId());
        entry.setSequenceNumber((short) 7);
        entry.setApplicationList(list);
        var code = new ResolutionCode();
        code.setResultCode("CODE\"1");
        var resolution = new AppListEntryResolution();
        resolution.setId(3L);
        resolution.setUuid(requestedItem.getResultId());
        resolution.setApplicationList(entry);
        resolution.setResolutionCode(code);
        resolution.setResolutionWording("Wording\\\"1");
        resolution.setResolutionOfficer("Officer\\\"1");

        assertThat(BulkDeleteApplicationEntryResultAudit.formatDeletedResults(List.of(resolution)))
                .contains(
                        "\"sequenceNumber\":7",
                        "CODE\\\"1",
                        "Wording\\\\\\\"1",
                        "Officer\\\\\\\"1");
    }
}

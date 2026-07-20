package uk.gov.hmcts.appregister.applicationentry.audit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditableData;
import uk.gov.hmcts.appregister.common.entity.AppListEntryFeeStatus;
import uk.gov.hmcts.appregister.common.entity.ApplicationList;
import uk.gov.hmcts.appregister.common.entity.ApplicationListEntry;
import uk.gov.hmcts.appregister.common.entity.TableNames;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;
import uk.gov.hmcts.appregister.common.enumeration.FeeStatusType;
import uk.gov.hmcts.appregister.generated.model.BulkFeeDetailsDto;
import uk.gov.hmcts.appregister.generated.model.PaymentStatus;

class BulkUpdateFeesAuditTest {

    @Test
    void extractAuditData_includesEntryMetadataFeeDetailsAndOffsiteEntries() {
        val listUuid = UUID.randomUUID();
        val entryId = UUID.randomUUID();
        val audit =
                new BulkUpdateFeesAudit(
                        10L,
                        listUuid,
                        List.of(entryId),
                        1,
                        "[{\"paymentStatus\":\"PAID\"}]",
                        "[\"" + entryId + "\"]");

        val auditData = audit.extractAuditData(CrudEnum.UPDATE);

        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_fees_list_uuid",
                        listUuid.toString()));
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_fees_fee_details",
                        "[{\"paymentStatus\":\"PAID\"}]"));
        Assertions.assertTrue(
                containsAuditRow(
                        auditData,
                        TableNames.APPLICATION_LISTS_ENTRY,
                        "bulk_fees_offsite_entry_ids",
                        "[\"" + entryId + "\"]"));
    }

    @Test
    void formatHelpers_renderExistingStatusesRequestedFeeDetailsAndOffsiteEntries() {
        val applicationList = new ApplicationList();
        applicationList.setId(10L);
        applicationList.setUuid(UUID.randomUUID());

        val entry = new ApplicationListEntry();
        entry.setId(100L);
        entry.setUuid(UUID.randomUUID());
        entry.setSequenceNumber((short) 2);
        entry.setApplicationList(applicationList);

        val existingStatus = new AppListEntryFeeStatus();
        existingStatus.setId(200L);
        existingStatus.setAppListEntry(entry);
        existingStatus.setAlefsFeeStatus(FeeStatusType.REMITTED);
        existingStatus.setAlefsFeeStatusDate(LocalDate.of(2026, 7, 20));
        existingStatus.setAlefsPaymentReference("PAY-001");

        val requestedFeeDetails =
                new BulkFeeDetailsDto()
                        .paymentStatus(PaymentStatus.PAID)
                        .statusDate(LocalDate.of(2026, 7, 20))
                        .paymentReference("PAY-002")
                        .hasOffsiteFee(true);

        Assertions.assertEquals(
                ("[{\"entryId\":\"%s\",\"status\":\"REMITTED\",\"statusDate\":\"2026-07-20\","
                                + "\"paymentReference\":\"PAY-001\"}]")
                        .formatted(entry.getUuid()),
                BulkUpdateFeesAudit.formatExistingFeeStatuses(List.of(existingStatus)));
        Assertions.assertEquals(
                "[{\"paymentStatus\":\"PAID\",\"statusDate\":\"2026-07-20\","
                        + "\"paymentReference\":\"PAY-002\",\"hasOffsiteFee\":true}]",
                BulkUpdateFeesAudit.formatRequestedFeeDetails(List.of(requestedFeeDetails)));
        Assertions.assertEquals(
                "[\"" + entry.getUuid() + "\"]",
                BulkUpdateFeesAudit.formatOffsiteEntryIds(
                        Set.of(entry.getId()), Map.of(entry.getId(), entry.getUuid())));
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
